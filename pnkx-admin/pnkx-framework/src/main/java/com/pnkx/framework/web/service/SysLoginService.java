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
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.random.NameAndHeader;
import com.pnkx.common.utils.template.TemplateUtils;
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

/**
 * 登录校验方法
 *
 * @author phy
 */
@Component
public class SysLoginService {
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
        // 生成token
        return tokenService.createToken(loginAction(userName, password));
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
        String activationTemplate = TemplateUtils.getTemplate("activation");
        activationTemplate = activationTemplate.replace("template-nickName", sysUser.getNickName());
        activationTemplate = activationTemplate.replace("template-url", WebsiteAddressConstants.WEB_SITE_ADDRESS + "login?activationUserName=" + sysUser.getUserName());
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
        SysEmail sysEmail = new SysEmail();
        String activationTemplate = TemplateUtils.getTemplate("rest");
        activationTemplate = activationTemplate.replace("template-userName", userName);
        activationTemplate = activationTemplate.replace("template-url", WebsiteAddressConstants.WEB_SITE_ADDRESS + "login?restUserName=" + userName);
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
}
