package com.pnkx.framework.web.service;

import com.pnkx.common.constant.Constants;
import com.pnkx.common.constant.UserConstants;
import com.pnkx.common.constant.WebsiteAddressConstants;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.core.redis.RedisCache;
import com.pnkx.common.exception.CustomException;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.exception.user.UserPasswordNotMatchException;
import com.pnkx.common.utils.MessageUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.ServletUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.ip.IpUtils;
import com.pnkx.common.utils.random.NameAndHeader;
import com.pnkx.common.utils.template.TemplateUtils;
import com.pnkx.common.utils.uuid.UUID;
import com.pnkx.framework.manager.AsyncManager;
import com.pnkx.framework.manager.factory.AsyncFactory;
import com.pnkx.system.domain.SysEmail;
import com.pnkx.system.service.ISysEmailService;
import com.pnkx.system.service.ISysUserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * 登录校验方法
 *
 * @author phy
 */
@Component
public class SysLoginService {

    /**
     * 账号激活令牌缓存前缀
     */
    private static final String ACTIVATION_TOKEN_KEY = "account_activation_token:";

    /**
     * 重置密码令牌缓存前缀
     */
    private static final String RESET_PASSWORD_TOKEN_KEY = "password_reset_token:";

    /**
     * 激活令牌有效期（小时）
     */
    private static final int ACTIVATION_TOKEN_EXPIRE_HOURS = 24;

    /**
     * 重置密码令牌有效期（分钟）
     */
    private static final int RESET_TOKEN_EXPIRE_MINUTES = 10;

    /**
     * 登录失败限流：同一账号+IP 连续失败达到阈值后临时锁定
     */
    private static final String LOGIN_FAIL_COUNT_KEY = "login_fail_count:";
    private static final int LOGIN_MAX_FAIL_COUNT = 5;
    private static final int LOGIN_LOCK_MINUTES = 10;

    @Resource
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private RedisCache redisCache;

    @Resource
    private ISysUserService userService;

    @Resource
    private ISysEmailService sysEmailService;

    /**
     * 登录验证
     *
     * @param userName 用户名
     * @param password 密码
     * @param code     验证码
     * @param uuid     唯一标识
     * @return 结果
     */
    public String login(String userName, String password, String code, String uuid) {
        return userNameAndPassWordLogin(userName, password);
    }

    /**
     * 博客客户端登录
     * @param userName 用户名
     * @param password 密码
     * @return
     */
    public String userNameAndPassWordLogin(String userName, String password) {
        // 防爆破：同一账号+IP 失败次数超限后临时锁定（管理端与客户端登录共用此入口）
        String failKey = LOGIN_FAIL_COUNT_KEY + userName + ":" + IpUtils.getIpAddr(ServletUtils.getRequest());
        Integer failCount = redisCache.getCacheObject(failKey);
        if (failCount != null && failCount >= LOGIN_MAX_FAIL_COUNT) {
            throw new ServiceException("登录失败次数过多，请" + LOGIN_LOCK_MINUTES + "分钟后重试");
        }
        try {
            // 生成token
            String token = tokenService.createToken(loginAction(userName, password));
            redisCache.deleteObject(failKey);
            return token;
        } catch (Exception e) {
            redisCache.setCacheObject(failKey, (failCount == null ? 1 : failCount + 1),
                    LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
            throw e;
        }
    }

    /**
     * 登录动作
     * @param userName
     * @param password
     * @return
     */
    public LoginUser loginAction(String userName, String password) {
        // 用户验证
        Authentication authentication;
        try {
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUserName
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(userName, password));
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            } else {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.LOGIN_FAIL, e.getMessage()));
                throw new CustomException(e.getMessage());
            }
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        return (LoginUser) authentication.getPrincipal();
    }

    /**
     * 注册
     */
    public void register(String userName, String password) throws Exception {
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(userName, password))
        {
            throw new ServiceException("用户/密码必须填写");
        }
        if (userName.length() < UserConstants.USERNAME_MIN_LENGTH
                || userName.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            throw new ServiceException("账户长度必须在2到20个字符之间");
        }
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            throw new ServiceException("密码长度必须在5到20个字符之间");
        }
        if (UserConstants.NOT_UNIQUE.equals(userService.checkUserNameUnique(userName))) {
            throw new ServiceException("注册用户'" + userName + "'失败，账号已存在");
        }

        // 注册用户信息
        SysUser sysUser = new SysUser();
        // 博客客户端分组
        sysUser.setDeptId(200L);
        sysUser.setUserName(userName);
        sysUser.setEmail(userName);
        sysUser.setNickName(NameAndHeader.randomName());
        sysUser.setAvatar(NameAndHeader.randomHeader());
        sysUser.setStatus("1");
        sysUser.setRoleIds(new Long[] {100L});
        sysUser.setPassword(SecurityUtils.encryptPassword(password));
        int insertUser = userService.insertUser(sysUser);
        if (insertUser < 1)
        {
            throw new ServiceException();
        }
        SysEmail sysEmail = new SysEmail();
        String activationToken = UUID.randomString(32);
        redisCache.setCacheObject(ACTIVATION_TOKEN_KEY + sysUser.getUserName(), activationToken,
                ACTIVATION_TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        String activationTemplate = TemplateUtils.getTemplate("activation");
        activationTemplate = activationTemplate.replace("template-nickName", sysUser.getNickName());
        activationTemplate = activationTemplate.replace("template-url", WebsiteAddressConstants.WEB_SITE_ADDRESS
                + "login?activationUserName=" + sysUser.getUserName() + "&activationToken=" + activationToken);
        sysEmail.setReceiverEmail(sysUser.getEmail());
        sysEmail.setSubject("新用户注册邮箱验证");
        sysEmail.setContent(activationTemplate);
        try {
            sysEmailService.sendMail(sysEmail);
        } catch (Exception e) {
            throw new ServiceException("发送邮件异常");
        }
    }

    public boolean sendResetEmail(String userName) throws Exception {
        SysUser sysUser = userService.selectUserByUserName(userName);
        if (sysUser == null) {
            // 账号不存在时静默成功，避免账号枚举
            return true;
        }
        String resetToken = UUID.randomString(32);
        redisCache.setCacheObject(RESET_PASSWORD_TOKEN_KEY + userName, resetToken,
                RESET_TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        SysEmail sysEmail = new SysEmail();
        String activationTemplate = TemplateUtils.getTemplate("rest");
        activationTemplate = activationTemplate.replace("template-userName", userName);
        activationTemplate = activationTemplate.replace("template-url", WebsiteAddressConstants.WEB_SITE_ADDRESS
                + "login?restUserName=" + userName + "&restToken=" + resetToken);
        sysEmail.setReceiverEmail(userName);
        sysEmail.setSubject("账号重置密码");
        sysEmail.setContent(activationTemplate);
        try {
            sysEmailService.sendMail(sysEmail);
        } catch (Exception e) {
            throw new ServiceException("发送邮件异常");
        }
        return true;
    }

    /**
     * 账号激活（校验邮件下发的一次性令牌）
     *
     * @param userName        用户名
     * @param activationToken 激活令牌
     * @return 激活结果
     */
    public boolean activation(String userName, String activationToken) {
        if (!consumeToken(ACTIVATION_TOKEN_KEY + userName, activationToken)) {
            return false;
        }
        SysUser sysUser = new SysUser();
        sysUser.setUserName(userName);
        sysUser.setStatus("0");
        return userService.updateUserByUserName(sysUser) > 0;
    }

    /**
     * 重置账号密码（校验邮件下发的一次性令牌）
     *
     * @param userName   用户名
     * @param resetToken 重置令牌
     * @return 新密码（令牌已验证邮箱归属，允许返回）
     */
    public String restPassword(String userName, String resetToken) {
        if (!consumeToken(RESET_PASSWORD_TOKEN_KEY + userName, resetToken)) {
            throw new ServiceException("重置链接无效或已过期，请重新发送重置邮件");
        }
        String newPassword = UUID.randomString(8);
        SysUser sysUser = new SysUser();
        sysUser.setUserName(userName);
        sysUser.setPassword(newPassword);
        if (userService.updateUserByUserName(sysUser) < 1) {
            throw new ServiceException("重置密码失败");
        }
        return newPassword;
    }

    /**
     * 校验并消费一次性令牌（常量时间比较，防止时序攻击）
     */
    private boolean consumeToken(String key, String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        String cached = redisCache.getCacheObject(key);
        if (cached == null) {
            return false;
        }
        boolean valid = MessageDigest.isEqual(
                cached.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
        if (valid) {
            redisCache.deleteObject(key);
        }
        return valid;
    }
}
