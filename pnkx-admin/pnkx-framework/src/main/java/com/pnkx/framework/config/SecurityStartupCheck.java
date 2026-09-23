package com.pnkx.framework.config;

import com.pnkx.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动期安全配置校验
 * <p>
 * 关键密钥缺失时快速失败（fail-fast），避免带病运行到首次请求才报出晦涩异常；
 * 可选功能密钥缺失时仅告警提示。
 *
 * @author phy
 */
@Component
public class SecurityStartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityStartupCheck.class);

    /**
     * HS512 要求密钥至少 64 字节
     */
    private static final int TOKEN_SECRET_MIN_BYTES = 64;

    @Value("${token.secret:}")
    private String tokenSecret;

    @Value("${wx.secret:}")
    private String wxSecret;

    @Value("${spring.datasource.druid.master.password:}")
    private String dbPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (StringUtils.isBlank(tokenSecret)) {
            throw new IllegalStateException(
                    "token.secret 未配置：请通过环境变量 TOKEN_SECRET 注入（HS512 至少 64 字符的随机串），服务拒绝启动");
        }
        if (tokenSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < TOKEN_SECRET_MIN_BYTES) {
            throw new IllegalStateException(
                    "token.secret 长度不足：HS512 签名要求至少 64 字节，请更换更长的随机密钥，服务拒绝启动");
        }
        if (StringUtils.isBlank(wxSecret)) {
            log.warn("wx.secret 未配置，微信小程序登录功能将不可用（如需启用请设置 WX_SECRET 环境变量）");
        }
        if ("123456".equals(dbPassword)) {
            log.warn("数据库正在使用默认弱密码 123456，请通过 DB_PASSWORD 环境变量配置强密码");
        }
    }
}
