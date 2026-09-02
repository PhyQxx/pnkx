package com.pnkx.framework.security.filter;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Set;

import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.framework.web.service.SysPermissionService;
import com.pnkx.system.service.ISysUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 内部系统集成令牌过滤器
 *
 * 供可信内部系统（如 Aria Companion Hub）以静态令牌访问 pnkx API：
 * 请求头 X-Integration-Token 与配置的 pnkx.integration.token 匹配时，
 * 以 pnkx.integration.userId 指定的用户身份建立 SecurityContext。
 *
 * 与账号密码登录相比：不存储/传递用户密码、无 Redis 会话过期、
 * 集成调用在审计上可绑定到专用账号；令牌泄露的处置就是改配置换令牌。
 * 未配置令牌时本过滤器完全不生效，行为与原先一致。
 *
 * @author phy
 */
@Component
public class IntegrationTokenFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(IntegrationTokenFilter.class);

    public static final String HEADER_NAME = "X-Integration-Token";

    @Value("${pnkx.integration.token:}")
    private String integrationToken;

    @Value("${pnkx.integration.userId:0}")
    private Long integrationUserId;

    @Resource
    private ISysUserService userService;

    @Resource
    private SysPermissionService permissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication existingAuth = SecurityUtils.getAuthentication();
        boolean noAuth = existingAuth == null || existingAuth instanceof AnonymousAuthenticationToken;
        String presented = request.getHeader(HEADER_NAME);

        if (noAuth && StringUtils.isNotEmpty(integrationToken) && StringUtils.isNotEmpty(presented)) {
            if (tokenMatches(presented)) {
                SysUser sysUser = userService.selectUserById(integrationUserId);
                if (sysUser != null && !"2".equals(sysUser.getDelFlag())) {
                    Set<String> permissions = permissionService.getMenuPermission(sysUser);
                    LoginUser loginUser = new LoginUser(sysUser, permissions);
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.debug("integration token accepted, bound to userId={}", integrationUserId);
                } else {
                    log.warn("integration token matched but userId={} not found", integrationUserId);
                }
            } else {
                log.warn("integration token rejected for {}", request.getRequestURI());
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 常量时间比较，避免时序侧信道
     */
    private boolean tokenMatches(String presented) {
        return MessageDigest.isEqual(
                integrationToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                presented.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
