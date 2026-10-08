package com.pnkx.framework.sso;

import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

/**
 * SSO 令牌签名密钥管理
 *
 * <p>首次启动生成 RSA 2048 密钥对并以 PEM 落库（sso_jwk），之后重启复用，
 * 保证 /oauth2/jwks 公钥稳定，各接入系统缓存的公钥不会因重启失效。
 *
 * @author phy
 */
@Service
public class SsoJwkService {

    private static final Logger log = LoggerFactory.getLogger(SsoJwkService.class);

    /**
     * 密钥行固定主键
     */
    private static final String KEY_ROW_ID = "main";

    /**
     * keyId 固定，便于接入系统按 kid 缓存
     */
    public static final String KEY_ID = "pnkx-sso-rsa-1";

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 加载签名密钥；不存在则生成并落库（多实例并发启动时依赖唯一键兜底回读）
     */
    public RSAKey loadOrCreateRsaKey() {
        try {
            RSAKey existing = loadFromDb();
            if (existing != null) {
                return existing;
            }
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            String publicKeyPem = encodePem("PUBLIC KEY", keyPair.getPublic().getEncoded());
            String privateKeyPem = encodePem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
            try {
                jdbcTemplate.update("INSERT INTO sso_jwk(id, public_key, private_key) VALUES (?, ?, ?)",
                        KEY_ROW_ID, publicKeyPem, privateKeyPem);
                log.info("SSO 签名密钥已生成并落库, keyId: {}", KEY_ID);
            } catch (DuplicateKeyException e) {
                log.info("SSO 签名密钥已被其他实例生成，回读复用");
            }
            RSAKey created = loadFromDb();
            if (created == null) {
                throw new IllegalStateException("SSO 签名密钥落库后读取失败");
            }
            return created;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("初始化 SSO 签名密钥失败", e);
        }
    }

    private RSAKey loadFromDb() {
        List<String[]> rows = jdbcTemplate.query(
                "SELECT public_key, private_key FROM sso_jwk WHERE id = ?",
                (rs, rowNum) -> new String[]{rs.getString(1), rs.getString(2)},
                KEY_ROW_ID);
        if (rows.isEmpty()) {
            return null;
        }
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(decodePem(rows.get(0)[0])));
            RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(decodePem(rows.get(0)[1])));
            return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(KEY_ID).build();
        } catch (Exception e) {
            throw new IllegalStateException("解析 SSO 签名密钥失败", e);
        }
    }

    private static String encodePem(String type, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }

    private static byte[] decodePem(String pem) {
        String body = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }
}
