package com.pnkx.web.controller.system;

import com.pnkx.common.constant.Constants;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.utils.random.NameAndHeader;
import com.pnkx.framework.web.service.SysPermissionService;
import com.pnkx.framework.web.service.TokenService;
import com.pnkx.system.service.ISysUserService;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * 微信小程序登录
 *
 * @author PHY
 */
@RestController
@RequestMapping("/wx")
public class WxLoginController {

    private static final Logger log = LoggerFactory.getLogger(WxLoginController.class);

    /**
     * code2Session 接口：用 code 换取 openid + session_key
     */
    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    /**
     * getPhoneNumber 接口：用 code 换取手机号（需 access_token）
     */
    private static final String ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String PHONE_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=%s";

    @Value("${wx.appid}")
    private String appid;

    @Value("${wx.secret}")
    private String secret;

    @Resource
    private ISysUserService userService;

    @Resource
    private TokenService tokenService;

    @Resource
    private SysPermissionService permissionService;

    /**
     * 微信登录（一键登录，通过 wx.login 的 code）
     * <p>
     * 流程：code → code2Session → openid → 查/建用户 → 签发 token。
     * 已绑定过 openid 的用户直接登录；新 openid 自动注册（昵称随机）。
     *
     * @param body {code: wx.login 的 code}
     */
    @PostMapping("/login")
    public AjaxResult wxLogin(@RequestBody Map<String, String> body) {
        String code = body == null ? null : body.get("code");
        if (code == null || code.trim().isEmpty()) {
            return AjaxResult.error("code 不能为空");
        }
        if (secret == null || secret.trim().isEmpty()) {
            return AjaxResult.error("未配置微信小程序 secret");
        }

        // 1. code 换 openid
        String openid = code2Session(code);
        if (openid == null) {
            return AjaxResult.error("微信登录失败：code 无效");
        }

        // 2. 查/建用户
        SysUser user = userService.selectUserByOpenid(openid);
        if (user == null) {
            user = createWxUser(openid);
            if (user == null) {
                return AjaxResult.error("注册失败，请重试");
            }
        }

        // 3. 签发 token
        String token = createTokenForUser(user);
        AjaxResult ajax = AjaxResult.success();
        ajax.put(Constants.TOKEN, token);
        ajax.put("isNew", user.getPhonenumber() == null);
        return ajax;
    }

    /**
     * 微信手机号授权登录（getPhoneNumber 的 code）
     * <p>
     * 流程：phoneCode → 手机号 → 若 openid 已登录则绑定手机号，
     * 否则按手机号查用户并绑定 openid。最终签发 token。
     * 需前端先 wx.login 拿到 loginCode，再 getPhoneNumber 拿到 phoneCode。
     *
     * @param body {loginCode: wx.login 的 code, phoneCode: getPhoneNumber 的 code}
     */
    @PostMapping("/phoneLogin")
    public AjaxResult phoneLogin(@RequestBody Map<String, String> body) {
        if (body == null) {
            return AjaxResult.error("参数为空");
        }
        String loginCode = body.get("loginCode");
        String phoneCode = body.get("phoneCode");
        if (phoneCode == null || phoneCode.trim().isEmpty()) {
            return AjaxResult.error("phoneCode 不能为空");
        }
        if (secret == null || secret.trim().isEmpty()) {
            return AjaxResult.error("未配置微信小程序 secret");
        }

        // 1. 取手机号
        String phone = getPhoneNumber(phoneCode);
        if (phone == null) {
            return AjaxResult.error("获取手机号失败");
        }

        SysUser user;
        // 2. 若带 loginCode，先换 openid
        if (loginCode != null && !loginCode.trim().isEmpty()) {
            String openid = code2Session(loginCode);
            if (openid != null) {
                // 优先按 openid 查
                user = userService.selectUserByOpenid(openid);
                if (user == null) {
                    // openid 未注册，按手机号查并绑定 openid
                    user = userService.selectUserByUserName(phone);
                }
                if (user == null) {
                    // 新用户：openid + 手机号注册
                    user = createWxUser(openid);
                    if (user != null) {
                        user.setPhonenumber(phone);
                        userService.updateUser(user);
                    }
                } else if (user.getOpenid() == null) {
                    // 老用户补绑 openid
                    user.setOpenid(openid);
                    userService.updateUser(user);
                }
            } else {
                return AjaxResult.error("微信登录失败：loginCode 无效");
            }
        } else {
            // 仅手机号登录：按手机号查
            user = userService.selectUserByUserName(phone);
            if (user == null) {
                user = createPhoneUser(phone);
            }
        }

        if (user == null) {
            return AjaxResult.error("登录失败，请重试");
        }

        String token = createTokenForUser(user);
        AjaxResult ajax = AjaxResult.success();
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * code2Session：code → openid
     */
    private String code2Session(String code) {
        try {
            String url = String.format(CODE2SESSION_URL, appid, secret, code);
            RestTemplate restTemplate = new RestTemplate();
            String result = restTemplate.getForObject(url, String.class);
            log.info("微信 code2Session 返回: {}", result);
            JSONObject json = JSONObject.fromObject(result);
            if (json.has("openid")) {
                return json.getString("openid");
            }
            log.error("code2Session 失败: {}", result);
        } catch (Exception e) {
            log.error("code2Session 异常", e);
        }
        return null;
    }

    /**
     * 获取小程序全局 access_token
     */
    private String getAccessToken() {
        try {
            String url = String.format(ACCESS_TOKEN_URL, appid, secret);
            RestTemplate restTemplate = new RestTemplate();
            String result = restTemplate.getForObject(url, String.class);
            JSONObject json = JSONObject.fromObject(result);
            if (json.has("access_token")) {
                return json.getString("access_token");
            }
            log.error("获取 access_token 失败: {}", result);
        } catch (Exception e) {
            log.error("获取 access_token 异常", e);
        }
        return null;
    }

    /**
     * getPhoneNumber：phoneCode → 手机号
     */
    private String getPhoneNumber(String phoneCode) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return null;
            }
            String url = String.format(PHONE_URL, accessToken);
            RestTemplate restTemplate = new RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            JSONObject reqBody = new JSONObject();
            reqBody.put("code", phoneCode);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(reqBody.toString(), headers);
            String result = restTemplate.postForObject(url, entity, String.class);
            log.info("微信 getPhoneNumber 返回: {}", result);
            JSONObject json = JSONObject.fromObject(result);
            if (json.has("phone_info")) {
                return json.getJSONObject("phone_info").getString("phoneNumber");
            }
            log.error("getPhoneNumber 失败: {}", result);
        } catch (Exception e) {
            log.error("getPhoneNumber 异常", e);
        }
        return null;
    }

    /**
     * 新建微信用户（openid 注册，随机昵称/头像）
     */
    private SysUser createWxUser(String openid) {
        SysUser user = new SysUser();
        user.setDeptId(200L);
        user.setOpenid(openid);
        user.setUserName("wx_" + openid.substring(0, Math.min(openid.length(), 16)));
        user.setNickName(NameAndHeader.randomName());
        user.setAvatar(NameAndHeader.randomHeader());
        user.setStatus("0");
        user.setRoleIds(new Long[]{100L});
        userService.insertUser(user);
        return userService.selectUserByOpenid(openid);
    }

    /**
     * 新建手机号用户
     */
    private SysUser createPhoneUser(String phone) {
        SysUser user = new SysUser();
        user.setDeptId(200L);
        user.setUserName(phone);
        user.setPhonenumber(phone);
        user.setNickName(NameAndHeader.randomName());
        user.setAvatar(NameAndHeader.randomHeader());
        user.setStatus("0");
        user.setRoleIds(new Long[]{100L});
        userService.insertUser(user);
        return userService.selectUserByUserName(phone);
    }

    /**
     * 为用户构造 LoginUser 并签发 token
     */
    private String createTokenForUser(SysUser user) {
        LoginUser loginUser = new LoginUser(user, permissionService.getMenuPermission(user));
        return tokenService.createToken(loginUser);
    }
}
