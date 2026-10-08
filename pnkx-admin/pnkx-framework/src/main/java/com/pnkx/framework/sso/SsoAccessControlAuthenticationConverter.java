package com.pnkx.framework.sso;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * 授权端点准入控制
 *
 * <p>包装默认的授权请求转换器：用户已通过 SSO 登录后，在进入授权流程前
 * 校验 sso_client_access 白名单，未命中则拒绝（特权系统只允许特定
 * 用户/角色登录）。未登录用户不拦截，走正常的登录重定向。
 *
 * @author phy
 */
public class SsoAccessControlAuthenticationConverter implements AuthenticationConverter {

    /**
     * 匿名用户标识（Spring Security 默认匿名 principal 名）
     */
    private static final String ANONYMOUS = "anonymousUser";

    private final OAuth2AuthorizationCodeRequestAuthenticationConverter delegate =
            new OAuth2AuthorizationCodeRequestAuthenticationConverter();

    private final SsoClientAccessService accessService;

    public SsoAccessControlAuthenticationConverter(SsoClientAccessService accessService) {
        this.accessService = accessService;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        Authentication authentication = delegate.convert(request);
        if (authentication == null) {
            return null;
        }
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        if (user == null || !user.isAuthenticated() || ANONYMOUS.equals(user.getName())) {
            // 未登录：放行，由授权链入口重定向到统一登录页
            return authentication;
        }
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationRequest =
                (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
        if (!accessService.isAllowed(authorizationRequest.getClientId(), user)) {
            throw new OAuth2AuthorizationCodeRequestAuthenticationException(
                    new OAuth2Error("access_denied", "当前账号无权访问该应用，请联系管理员", null),
                    authorizationRequest);
        }
        return authentication;
    }
}
