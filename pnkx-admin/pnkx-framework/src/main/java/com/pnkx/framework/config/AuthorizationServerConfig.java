package com.pnkx.framework.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.framework.sso.SsoAccessControlAuthenticationConverter;
import com.pnkx.framework.sso.SsoLoginUrlEntryPoint;
import com.pnkx.framework.sso.SsoClaimsService;
import com.pnkx.framework.sso.SsoClientAccessService;
import com.pnkx.framework.sso.SsoJwkService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * OAuth2/OIDC 授权服务器配置（SSO 身份源）
 *
 * <p>pnkx 作为唯一 IdP：/oauth2/*、/userinfo、/connect/* 走本链（Order 1），
 * /sso/** 统一登录页也归本链处理（会话走 spring-session Redis）；
 * 其余请求仍走原有 JWT 业务链（SecurityConfig），两套互不干扰。
 *
 * <p>用户源复用 sys_user（DaoAuthenticationProvider + BCrypt），
 * 授权记录、客户端注册、令牌元数据均落 MySQL，重启不丢。
 *
 * @author phy
 */
@Configuration
public class AuthorizationServerConfig {

    @Value("${pnkx.sso.issuer:}")
    private String issuer;

    @Value("${pnkx.sso.login-page:/sso/login}")
    private String loginPage;

    @Value("${pnkx.sso.gateway-prefix:}")
    private String gatewayPrefix;

    /**
     * 授权服务器过滤链（含统一登录页），优先于业务 JWT 链
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http, SsoClientAccessService clientAccessService) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        RequestMatcher endpointsMatcher = new OrRequestMatcher(
                authorizationServerConfigurer.getEndpointsMatcher(),
                new AntPathRequestMatcher("/sso/**"));

        http
                .securityMatcher(endpointsMatcher)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/sso/**").permitAll()
                        .anyRequest().authenticated())
                // 登录表单走 /sso/doLogin 自研端点，CSRF 由 state 参数与防爆破兜底
                .csrf(AbstractHttpConfigurer::disable)
                // SSO 登录态存 Redis session（spring-session-data-redis）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(securityContext -> securityContext.securityContextRepository(securityContextRepository()))
                // 未认证的授权请求重定向到统一登录页（登录页路径/网关前缀可配，
                // 适配 /prod-api 等带前缀反代，详见 SsoLoginUrlEntryPoint）
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new SsoLoginUrlEntryPoint(loginPage, gatewayPrefix),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                // /userinfo 端点接受 Bearer JWT
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .with(authorizationServerConfigurer, authorizationServer -> authorizationServer
                        // OIDC：discovery + userinfo；userinfo 声明直接取自定制过的 access token
                        .oidc(oidc -> oidc
                                .userInfoEndpoint(userInfo -> userInfo.userInfoMapper(this::mapUserInfo)))
                        // 授权端点准入：client 级白名单
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestConverter(
                                new SsoAccessControlAuthenticationConverter(clientAccessService))));

        return http.build();
    }

    /**
     * SSO 登录态仓库：HttpSession（spring-session 已将其接到 Redis）
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * 令牌声明定制：ID Token 与（openid 范围的）Access Token 注入 sys_user 用户信息
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> ssoTokenCustomizer(SsoClaimsService claimsService) {
        return context -> {
            boolean idToken = OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue());
            boolean accessToken = OAuth2TokenType.ACCESS_TOKEN.getValue().equals(context.getTokenType().getValue())
                    && context.getAuthorizedScopes().contains(OidcScopes.OPENID);
            if (idToken || accessToken) {
                claimsService.applyClaims(context.getClaims(), context.getPrincipal().getName());
            }
        };
    }

    /**
     * /userinfo 响应：复用 access token 中的声明（登录时已由定制器写入）
     */
    private OidcUserInfo mapUserInfo(OidcUserInfoAuthenticationContext context) {
        Object principal = context.getAuthentication().getPrincipal();
        if (principal instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtPrincipal) {
            return new OidcUserInfo(jwtPrincipal.getToken().getClaims());
        }
        return new OidcUserInfo(java.util.Collections.emptyMap());
    }

    /**
     * 客户端注册仓库（MySQL）
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    /**
     * 授权服务器设置：issuer 留空则按请求域名自动推断（本地调试友好）
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        AuthorizationServerSettings.Builder builder = AuthorizationServerSettings.builder();
        if (StringUtils.isNotBlank(issuer)) {
            builder.issuer(issuer);
        }
        return builder.build();
    }

    /**
     * 签名密钥（PEM 落库，重启不变），jwks 端点据此发布公钥
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource(SsoJwkService jwkService) {
        RSAKey rsaKey = jwkService.loadOrCreateRsaKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    /**
     * JDBC 持久化的授权服务（授权码/令牌记录）与授权同意服务
     */
    @Configuration(proxyBeanMethods = false)
    public static class AuthorizationServerJdbcConfig {

        @Bean
        public OAuth2AuthorizationService oauth2AuthorizationService(
                JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
            return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        }

        @Bean
        public OAuth2AuthorizationConsentService oauth2AuthorizationConsentService(
                JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
            return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
        }
    }
}
