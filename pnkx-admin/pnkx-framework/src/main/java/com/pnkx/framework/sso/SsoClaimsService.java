package com.pnkx.framework.sso;

import com.pnkx.common.constant.WebsiteAddressConstants;
import com.pnkx.common.core.domain.entity.SysRole;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.system.service.ISysUserService;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SSO 令牌声明（claims）装配
 *
 * <p>把 sys_user 用户信息映射为标准 OIDC claims，供 ID Token、Access Token
 * 和 /userinfo 端点统一使用。sub 固定为 pnkx userId，是各接入系统做账号
 * 关联（sso_id）的唯一键。
 *
 * @author phy
 */
@Service
public class SsoClaimsService {

    @Resource
    private ISysUserService userService;

    /**
     * 为令牌声明追加用户信息（用户不存在时保持默认，仅含 principal 名）
     *
     * @param claims   声明构建器
     * @param userName 登录用户名（SSO 会话 principal 名）
     */
    public void applyClaims(JwtClaimsSet.Builder claims, String userName) {
        SysUser user = userService.selectUserByUserName(userName);
        if (user == null) {
            return;
        }
        claims.claim("sub", String.valueOf(user.getUserId()));
        claimIfPresent(claims, "preferred_username", user.getUserName());
        claimIfPresent(claims, "name", user.getNickName());
        // OIDC 标准 claim；Jpom（JustAuth TopIAM 平台）以 nickname 作显示名
        claimIfPresent(claims, "nickname", user.getNickName());
        if (StringUtils.isNotBlank(user.getEmail())) {
            claims.claim("email", user.getEmail());
            claims.claim("email_verified", true);
        }
        claimIfPresent(claims, "phone_number", user.getPhonenumber());
        claimIfPresent(claims, "picture", absoluteAvatar(user.getAvatar()));
        claims.claim("roles", roleKeys(user));
    }

    /**
     * JwtClaimsSet 不接受 null 值，空值声明直接跳过
     */
    private void claimIfPresent(JwtClaimsSet.Builder claims, String name, String value) {
        if (StringUtils.isNotBlank(value)) {
            claims.claim(name, value);
        }
    }

    /**
     * 头像转绝对地址（RuoYi 风格头像常存为 /profile/... 相对路径）
     */
    private String absoluteAvatar(String avatar) {
        if (StringUtils.isBlank(avatar)) {
            return null;
        }
        if (avatar.startsWith("http://") || avatar.startsWith("https://")) {
            return avatar;
        }
        String site = WebsiteAddressConstants.WEB_SITE_ADDRESS;
        String path = avatar.startsWith("/") ? avatar : "/" + avatar;
        return site.endsWith("/") ? site.substring(0, site.length() - 1) + path : site + path;
    }

    private List<String> roleKeys(SysUser user) {
        if (user.getRoles() == null) {
            return Collections.emptyList();
        }
        return user.getRoles().stream()
                .map(SysRole::getRoleKey)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }
}
