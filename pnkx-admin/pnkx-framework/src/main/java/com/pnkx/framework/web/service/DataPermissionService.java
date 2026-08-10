package com.pnkx.framework.web.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.system.service.ISysDataGroupService;

/**
 * 数据权限服务：供业务层显式获取当前登录用户的可见数据范围。
 * <p>
 * 用于无法使用 {@link com.pnkx.common.annotation.DataScopeSelf} 注解的场景
 * （例如参数为 Map、无参数的聚合统计查询）。
 *
 * @author pnkx
 */
@Service
public class DataPermissionService {

    /**
     * 管理员角色标识
     */
    private static final String ADMIN_ROLE_KEY = "admin";

    @Autowired
    private ISysDataGroupService dataGroupService;

    /**
     * 当前登录用户是否拥有全部数据权限（超管 / admin 角色）
     */
    public boolean isDataScopeAll() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNull(loginUser)) {
            return false;
        }
        SysUser user = loginUser.getUser();
        if (StringUtils.isNull(user)) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        if (loginUser.getPermissions() != null && loginUser.getPermissions().contains(ADMIN_ROLE_KEY)) {
            return true;
        }
        if (user.getRoles() != null) {
            return user.getRoles().stream()
                    .anyMatch(r -> ADMIN_ROLE_KEY.equals(r.getRoleKey()));
        }
        return false;
    }

    /**
     * 获取当前登录用户可见的 userId 集合（自己 + 所在群组成员）。
     * 管理员返回 null（调用方据此跳过过滤）。
     *
     * @return 可见 userId 集合；为 null 表示不限数据权限
     */
    public List<Long> getVisibleUserIds() {
        if (isDataScopeAll()) {
            return null;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNull(loginUser) || StringUtils.isNull(loginUser.getUser())) {
            return Collections.emptyList();
        }
        Long userId = loginUser.getUser().getUserId();
        if (StringUtils.isNull(userId)) {
            return Collections.emptyList();
        }
        return dataGroupService.selectVisibleUserIds(userId);
    }
}
