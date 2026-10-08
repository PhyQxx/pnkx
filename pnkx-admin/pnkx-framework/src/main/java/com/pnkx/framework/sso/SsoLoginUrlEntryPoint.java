package com.pnkx.framework.sso;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * SSO 统一登录入口点：未认证的授权请求重定向到统一登录页，
 * 并携带 continue 参数（登录后按原路返回授权端点）。
 *
 * <p>后端若经带前缀的反代（如 Lucky/nginx 的 /prod-api → 8068）暴露，
 * 后端看到的请求路径不含前缀，直接重定向 /sso/login 会落到网关的 SPA
 * 兜底上，因此由 gateway-prefix 配置补回前缀。
 *
 * @author phy
 */
public class SsoLoginUrlEntryPoint implements AuthenticationEntryPoint {

    private final String loginPage;

    private final String gatewayPrefix;

    public SsoLoginUrlEntryPoint(String loginPage, String gatewayPrefix) {
        this.loginPage = loginPage;
        this.gatewayPrefix = gatewayPrefix == null ? "" : gatewayPrefix;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException {
        String target = gatewayPrefix + request.getRequestURI()
                + (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        String location = loginPage + "?continue="
                + URLEncoder.encode(target, StandardCharsets.UTF_8);
        response.sendRedirect(location);
    }
}
