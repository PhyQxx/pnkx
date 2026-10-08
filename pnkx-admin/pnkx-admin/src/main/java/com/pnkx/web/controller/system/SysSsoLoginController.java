package com.pnkx.web.controller.system;

import com.pnkx.common.core.domain.entity.SysRole;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.framework.web.service.SysLoginService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * SSO 统一登录页
 *
 * <p>所有接入系统的授权请求在未登录时重定向到 /sso/login。
 * 登录校验复用 SysLoginService（BCrypt + 防爆破 + 登录日志）；
 * 认证成功后写入 Redis 会话（spring-session），principal 使用
 * 标准 Spring User（保证授权记录可经 Jackson 落 MySQL）。
 *
 * @author phy
 */
@Controller
@RequestMapping("/sso")
public class SysSsoLoginController {

    private static final String ANONYMOUS = "anonymousUser";

    /**
     * 统一登录页路径（经 /prod-api 等带前缀反代时由环境变量覆盖）
     */
    @Value("${pnkx.sso.login-page:/sso/login}")
    private String loginPage;

    @Resource
    private SysLoginService sysLoginService;

    private final RequestCache requestCache = new NullRequestCache();

    /**
     * 统一登录页；已有 SSO 会话则直接跳转回目标地址
     */
    @GetMapping("/login")
    public void loginPage(@RequestParam(value = "continue", required = false) String continueUrl,
                          @RequestParam(value = "error", required = false) String error,
                          @RequestParam(value = "loggedout", required = false) String loggedout,
                          HttpServletRequest request, HttpServletResponse response) throws IOException {
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated() && !ANONYMOUS.equals(existing.getName())) {
            response.sendRedirect(safeContinue(continueUrl, loginPage));
            return;
        }
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(renderLoginPage(safeContinue(continueUrl, loginPage), error, loggedout != null));
    }

    /**
     * 登录处理（表单提交）
     */
    @PostMapping("/doLogin")
    public void doLogin(@RequestParam(value = "username", required = false) String username,
                        @RequestParam(value = "password", required = false) String password,
                        @RequestParam(value = "continue", required = false) String continueUrl,
                        HttpServletRequest request, HttpServletResponse response) throws IOException {
        String target = safeContinue(continueUrl, "/");
        if (StringUtils.isAnyBlank(username, password)) {
            redirectError(response, target);
            return;
        }
        try {
            LoginUser loginUser = sysLoginService.guardedLogin(username.trim(), password);
            establishSession(loginUser, request);
            // 始终回 continue 指向的授权请求（入口点已带前缀编码），不再依赖请求缓存
            response.sendRedirect(target);
        } catch (Exception e) {
            redirectError(response, target);
        }
    }

    /**
     * 退出 SSO 会话
     */
    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        response.sendRedirect(loginPage + "?loggedout=1");
    }

    /**
     * 建立 SSO 会话：写入经序列化友好的轻量 principal（标准 User + 角色），
     * 会话固定防护（更换 sessionId）后落 Redis
     */
    private void establishSession(LoginUser loginUser, HttpServletRequest request) {
        SysUser user = loginUser.getUser();
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRoles() != null) {
            for (SysRole role : user.getRoles()) {
                if (StringUtils.isNotBlank(role.getRoleKey())) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleKey()));
                }
            }
        }
        org.springframework.security.core.userdetails.UserDetails principal =
                org.springframework.security.core.userdetails.User.withUsername(user.getUserName())
                        .password("")
                        .authorities(authorities)
                        .build();
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);

        HttpSession session = request.getSession(true);
        // 会话固定防护：先建会话再换 ID，最后写入认证上下文
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private void redirectError(HttpServletResponse response, String target) throws IOException {
        String url = loginPage + "?error=1";
        if (!"/".equals(target)) {
            url += "&continue=" + URLEncoder.encode(target, StandardCharsets.UTF_8);
        }
        response.sendRedirect(url);
    }

    /**
     * 仅允许站内相对地址，防开放重定向
     */
    private String safeContinue(String continueUrl, String fallback) {
        if (StringUtils.isBlank(continueUrl) || !continueUrl.startsWith("/") || continueUrl.startsWith("//")) {
            return StringUtils.isBlank(fallback) ? "/" : fallback;
        }
        return continueUrl;
    }

    /**
     * 登录页 HTML（自包含样式，无外部依赖）
     */
    private String renderLoginPage(String continueUrl, String error, boolean loggedout) {
        String errorBanner = "";
        if (error != null) {
            errorBanner = """
                    <div class="banner error">账号或密码错误，或失败次数过多被临时锁定<br><span>连续失败 5 次将锁定 10 分钟</span></div>""";
        } else if (loggedout) {
            errorBanner = """
                    <div class="banner info">已安全退出，期待再次相见</div>""";
        }
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>统一登录 · Pei你看雪</title>
                <style>
                  * { margin: 0; padding: 0; box-sizing: border-box; }
                  body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
                    min-height: 100vh; display: flex; align-items: center; justify-content: center;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                  }
                  .card {
                    width: 380px; max-width: calc(100vw - 32px); background: #fff; border-radius: 16px;
                    padding: 40px 36px 28px; box-shadow: 0 20px 60px rgba(0,0,0,.3);
                  }
                  .brand { text-align: center; margin-bottom: 8px; font-size: 15px; color: #764ba2; font-weight: 600; letter-spacing: 2px; }
                  h1 { text-align: center; font-size: 22px; color: #303133; margin-bottom: 26px; font-weight: 600; }
                  .field { margin-bottom: 18px; }
                  .field label { display: block; font-size: 13px; color: #606266; margin-bottom: 6px; }
                  .field input {
                    width: 100%; height: 44px; border: 1px solid #dcdfe6; border-radius: 8px;
                    padding: 0 14px; font-size: 15px; outline: none; transition: border-color .2s;
                  }
                  .field input:focus { border-color: #764ba2; }
                  button {
                    width: 100%; height: 46px; margin-top: 6px; border: none; border-radius: 8px;
                    background: linear-gradient(135deg, #667eea, #764ba2); color: #fff;
                    font-size: 16px; cursor: pointer; transition: opacity .2s;
                  }
                  button:hover { opacity: .9; }
                  .banner { border-radius: 8px; padding: 10px 14px; font-size: 13px; margin-bottom: 18px; line-height: 1.6; }
                  .banner.error { background: #fef0f0; color: #f56c6c; border: 1px solid #fde2e2; }
                  .banner.info { background: #f0f9eb; color: #67c23a; border: 1px solid #e1f3d8; }
                  .banner span { opacity: .75; font-size: 12px; }
                  .foot { margin-top: 22px; text-align: center; font-size: 12px; color: #909399; }
                </style>
                </head>
                <body>
                  <div class="card">
                    <div class="brand">PEI 你看雪</div>
                    <h1>统一登录</h1>
                    __ERROR_BANNER__
                    <form method="post" action="/sso/doLogin" autocomplete="on">
                      <input type="hidden" name="continue" value="__CONTINUE__">
                      <div class="field">
                        <label>账号</label>
                        <input type="text" name="username" placeholder="用户名 / 注册邮箱" required autofocus>
                      </div>
                      <div class="field">
                        <label>密码</label>
                        <input type="password" name="password" placeholder="请输入密码" required>
                      </div>
                      <button type="submit">登 录</button>
                    </form>
                    <div class="foot">登录后将返回来源应用 · 账号即本站（pnkx）账号</div>
                  </div>
                </body>
                </html>
                """
                .replace("__ERROR_BANNER__", errorBanner)
                .replace("__CONTINUE__", escapeHtml(continueUrl));
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
