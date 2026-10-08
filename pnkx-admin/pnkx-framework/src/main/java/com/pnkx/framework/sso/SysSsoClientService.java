package com.pnkx.framework.sso;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SSO 接入应用管理
 *
 * <p>基于 Spring Authorization Server 的 RegisteredClientRepository 做
 * 增删改查（写入走官方 Jdbc 实现，保证 settings 序列化正确）；
 * 密钥用 BCrypt 存储，与客户端认证使用的 PasswordEncoder 一致。
 *
 * @author phy
 */
@Service
public class SysSsoClientService {

    /**
     * client_id 允许的字符：字母数字下划线连字符，2-50 位
     */
    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{2,50}$");

    /**
     * 默认 access token 有效期（分钟）
     */
    private static final long DEFAULT_ACCESS_TOKEN_MINUTES = 30;

    /**
     * 默认 refresh token 有效期（分钟）
     */
    private static final long DEFAULT_REFRESH_TOKEN_MINUTES = 1440;

    @Resource
    private RegisteredClientRepository registeredClientRepository;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Resource
    private SsoClientAccessService accessService;

    /**
     * 应用列表（含白名单规则）
     */
    public List<SysSsoClient> listClients() {
        List<String> clientIds = jdbcTemplate.query(
                "SELECT client_id FROM oauth2_registered_client ORDER BY client_id",
                (rs, rowNum) -> rs.getString(1));
        List<SysSsoClient> result = new ArrayList<>();
        for (String clientId : clientIds) {
            RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
            if (registeredClient != null) {
                SysSsoClient dto = toDto(registeredClient);
                dto.setAccessRules(accessService.listRules(clientId));
                result.add(dto);
            }
        }
        result.sort(Comparator.comparing(SysSsoClient::getClientId));
        return result;
    }

    /**
     * 新增应用
     *
     * @return 生成的一次性明文密钥（公共客户端返回空）
     */
    public String addClient(SysSsoClient dto) {
        validate(dto, true);
        String rawSecret = null;
        RegisteredClient.Builder builder = RegisteredClient.withId(java.util.UUID.randomUUID().toString())
                .clientId(dto.getClientId())
                .clientName(StringUtils.isBlank(dto.getClientName()) ? dto.getClientId() : dto.getClientName())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE);
        if (dto.getScopes() != null) {
            dto.getScopes().stream().filter(StringUtils::isNotBlank)
                    .filter(s -> !OidcScopes.OPENID.equals(s) && !OidcScopes.PROFILE.equals(s))
                    .forEach(builder::scope);
        }
        if (dto.getRedirectUris() != null) {
            builder.redirectUris(uris -> uris.addAll(dto.getRedirectUris()));
        }
        if (dto.isPublicClient()) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        } else {
            if (StringUtils.isBlank(dto.getClientSecret()) || dto.getClientSecret().length() < 16) {
                throw new ServiceException("密钥至少 16 个字符");
            }
            rawSecret = dto.getClientSecret();
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .clientSecret(passwordEncoder.encode(rawSecret));
        }
        builder.clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(dto.isPublicClient())
                .build());
        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(defaultIfNull(dto.getAccessTokenMinutes(), DEFAULT_ACCESS_TOKEN_MINUTES)))
                .refreshTokenTimeToLive(Duration.ofMinutes(defaultIfNull(dto.getRefreshTokenMinutes(), DEFAULT_REFRESH_TOKEN_MINUTES)))
                .build());
        registeredClientRepository.save(builder.build());
        accessService.replaceRules(dto.getClientId(), dto.getAccessRules());
        return rawSecret;
    }

    /**
     * 修改应用（client_id 不可改；密钥留空表示不变更，传入新值则轮换）
     *
     * @return 轮换后的明文密钥（未轮换返回空）
     */
    public String updateClient(SysSsoClient dto) {
        if (StringUtils.isBlank(dto.getId())) {
            throw new ServiceException("缺少应用主键");
        }
        RegisteredClient existing = registeredClientRepository.findById(dto.getId());
        if (existing == null) {
            throw new ServiceException("应用不存在或已被删除");
        }
        validate(dto, false);
        String rotatedSecret = null;
        RegisteredClient.Builder builder = RegisteredClient.from(existing)
                .clientName(StringUtils.isBlank(dto.getClientName()) ? existing.getClientId() : dto.getClientName());
        if (dto.getRedirectUris() != null) {
            // from(existing) 会带上旧回调，先清再放，保证与表单一致
            builder.redirectUris(uris -> {
                uris.clear();
                dto.getRedirectUris().stream().filter(StringUtils::isNotBlank).forEach(uris::add);
            });
        }
        if (dto.getScopes() != null) {
            builder.scopes(scopes -> {
                scopes.clear();
                scopes.add(OidcScopes.OPENID);
                scopes.add(OidcScopes.PROFILE);
                dto.getScopes().stream().filter(StringUtils::isNotBlank)
                        .filter(s -> !OidcScopes.OPENID.equals(s) && !OidcScopes.PROFILE.equals(s))
                        .forEach(scopes::add);
            });
        }
        if (StringUtils.isNotBlank(dto.getClientSecret())) {
            if (dto.getClientSecret().length() < 16) {
                throw new ServiceException("密钥至少 16 个字符");
            }
            if (!dto.isPublicClient()) {
                rotatedSecret = dto.getClientSecret();
                builder.clientSecret(passwordEncoder.encode(rotatedSecret));
            }
        }
        ClientSettings.Builder clientSettings = ClientSettings.withSettings(existing.getClientSettings().getSettings())
                .requireAuthorizationConsent(false)
                .requireProofKey(dto.isPublicClient());
        builder.clientSettings(clientSettings.build());
        TokenSettings.Builder tokenSettings = TokenSettings.withSettings(existing.getTokenSettings().getSettings())
                .accessTokenTimeToLive(Duration.ofMinutes(defaultIfNull(dto.getAccessTokenMinutes(),
                        existing.getTokenSettings().getAccessTokenTimeToLive().toMinutes())))
                .refreshTokenTimeToLive(Duration.ofMinutes(defaultIfNull(dto.getRefreshTokenMinutes(),
                        toMinutes(existing.getTokenSettings().getRefreshTokenTimeToLive()))));
        builder.tokenSettings(tokenSettings.build());
        registeredClientRepository.save(builder.build());
        accessService.replaceRules(existing.getClientId(), dto.getAccessRules());
        return rotatedSecret;
    }

    /**
     * 删除应用（连同授权记录、同意记录、白名单）
     */
    public void deleteClient(String id) {
        RegisteredClient existing = registeredClientRepository.findById(id);
        if (existing == null) {
            return;
        }
        jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE registered_client_id = ?", id);
        jdbcTemplate.update("DELETE FROM oauth2_authorization_consent WHERE registered_client_id = ?", id);
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE id = ?", id);
        accessService.deleteRules(existing.getClientId());
    }

    private void validate(SysSsoClient dto, boolean forCreate) {
        if (forCreate) {
            if (StringUtils.isBlank(dto.getClientId()) || !CLIENT_ID_PATTERN.matcher(dto.getClientId()).matches()) {
                throw new ServiceException("client_id 仅允许字母数字下划线连字符，长度 2-50");
            }
            if (registeredClientRepository.findByClientId(dto.getClientId()) != null) {
                throw new ServiceException("client_id 已存在");
            }
        }
        if (dto.getRedirectUris() == null || dto.getRedirectUris().isEmpty()) {
            throw new ServiceException("至少配置一个回调地址");
        }
        for (String uri : dto.getRedirectUris()) {
            if (StringUtils.isBlank(uri) || !(uri.startsWith("http://") || uri.startsWith("https://"))) {
                throw new ServiceException("回调地址必须是 http(s) 地址: " + uri);
            }
        }
    }

    private SysSsoClient toDto(RegisteredClient registeredClient) {
        SysSsoClient dto = new SysSsoClient();
        dto.setId(registeredClient.getId());
        dto.setClientId(registeredClient.getClientId());
        dto.setClientName(registeredClient.getClientName());
        dto.setRedirectUris(new ArrayList<>(registeredClient.getRedirectUris()));
        dto.setScopes(registeredClient.getScopes().stream()
                .filter(s -> !OidcScopes.OPENID.equals(s) && !OidcScopes.PROFILE.equals(s))
                .collect(java.util.stream.Collectors.toList()));
        dto.setPublicClient(registeredClient.getClientAuthenticationMethods()
                .contains(ClientAuthenticationMethod.NONE));
        dto.setAccessTokenMinutes(registeredClient.getTokenSettings().getAccessTokenTimeToLive().toMinutes());
        dto.setRefreshTokenMinutes(toMinutes(registeredClient.getTokenSettings().getRefreshTokenTimeToLive()));
        return dto;
    }

    private static long toMinutes(Duration duration) {
        return duration == null ? DEFAULT_REFRESH_TOKEN_MINUTES : duration.toMinutes();
    }

    private static long defaultIfNull(Long value, long defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    /**
     * 应用注册信息（管理界面对象）
     */
    public static class SysSsoClient {
        /**
         * 内部主键（编辑/删除用）
         */
        private String id;

        /**
         * 客户端标识（创建后不可改）
         */
        private String clientId;

        /**
         * 应用名称
         */
        private String clientName;

        /**
         * 密钥：新增必填（非公共客户端）；编辑留空=不变更
         */
        private String clientSecret;

        /**
         * 回调地址列表（精确匹配白名单）
         */
        private List<String> redirectUris;

        /**
         * 自定义授权范围（openid/profile 固定附带）
         */
        private List<String> scopes;

        /**
         * 公共客户端（SPA/App）：强制 PKCE、无密钥
         */
        private boolean publicClient;

        /**
         * access token 有效期（分钟）
         */
        private Long accessTokenMinutes;

        /**
         * refresh token 有效期（分钟）
         */
        private Long refreshTokenMinutes;

        /**
         * 访问白名单（空=所有用户可登录）
         */
        private List<SsoClientAccessService.SsoClientAccessRule> accessRules;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientName() {
            return clientName;
        }

        public void setClientName(String clientName) {
            this.clientName = clientName;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public List<String> getRedirectUris() {
            return redirectUris;
        }

        public void setRedirectUris(List<String> redirectUris) {
            this.redirectUris = redirectUris;
        }

        public List<String> getScopes() {
            return scopes;
        }

        public void setScopes(List<String> scopes) {
            this.scopes = scopes;
        }

        public boolean isPublicClient() {
            return publicClient;
        }

        public void setPublicClient(boolean publicClient) {
            this.publicClient = publicClient;
        }

        public Long getAccessTokenMinutes() {
            return accessTokenMinutes;
        }

        public void setAccessTokenMinutes(Long accessTokenMinutes) {
            this.accessTokenMinutes = accessTokenMinutes;
        }

        public Long getRefreshTokenMinutes() {
            return refreshTokenMinutes;
        }

        public void setRefreshTokenMinutes(Long refreshTokenMinutes) {
            this.refreshTokenMinutes = refreshTokenMinutes;
        }

        public List<SsoClientAccessService.SsoClientAccessRule> getAccessRules() {
            return accessRules;
        }

        public void setAccessRules(List<SsoClientAccessService.SsoClientAccessRule> accessRules) {
            this.accessRules = accessRules;
        }
    }
}
