package com.pnkx.framework.aspectj;

import java.lang.reflect.Method;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.core.domain.BaseEntity;
import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.utils.ServletUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.spring.SpringUtils;
import com.pnkx.framework.web.service.TokenService;
import com.pnkx.system.service.ISysDataGroupService;

/**
 * 个人数据权限过滤处理。
 * <p>
 * 与若依原版 {@code DataScopeAspect}（基于部门）互补，本切面基于「用户本人 + 群组成员」。
 *
 * @author pnkx
 */
@Aspect
@Component
public class DataScopeSelfAspect {

    /**
     * 管理员角色标识（超管和拥有 admin 角色的用户都不限数据）
     */
    private static final String ADMIN_ROLE_KEY = "admin";

    // 配置织入点
    @Pointcut("@annotation(com.pnkx.common.annotation.DataScopeSelf)")
    public void dataScopeSelfPointCut() {
    }

    @Before("dataScopeSelfPointCut()")
    public void doBefore(JoinPoint point) throws Throwable {
        handleDataScope(point);
    }

    protected void handleDataScope(final JoinPoint joinPoint) {
        DataScopeSelf dataScope = getAnnotation(joinPoint);
        if (dataScope == null) {
            return;
        }
        LoginUser loginUser = SpringUtils.getBean(TokenService.class).getLoginUser(ServletUtils.getRequest());
        if (StringUtils.isNull(loginUser)) {
            return;
        }
        SysUser currentUser = loginUser.getUser();
        if (StringUtils.isNull(currentUser)) {
            return;
        }
        // 兼容 MyBatis-Plus 分页重载：方法签名可能是 selectXxxList(entity)，
        // 也可能是 selectXxxList(IPage page, entity)，因此遍历参数找到第一个 BaseEntity。
        BaseEntity baseEntity = null;
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof BaseEntity) {
                baseEntity = (BaseEntity) arg;
                break;
            }
        }
        if (baseEntity == null) {
            return;
        }

        // 超管 / 拥有 admin 角色 → 不限数据
        if (isDataScopeAll(currentUser, loginUser)) {
            baseEntity.getParams().put(DataScopeSelf.SCOPE_ALL, true);
            return;
        }

        // 受限：自己 + 所在群组成员
        List<Long> visibleUserIds = SpringUtils.getBean(ISysDataGroupService.class)
                .selectVisibleUserIds(currentUser.getUserId());
        baseEntity.getParams().put(DataScopeSelf.SCOPE_ALL, false);
        baseEntity.getParams().put(DataScopeSelf.SCOPE_USER_IDS, visibleUserIds);
    }

    /**
     * 是否拥有全部数据权限：超管或拥有 admin 角色标识
     */
    private boolean isDataScopeAll(SysUser user, LoginUser loginUser) {
        if (user.isAdmin()) {
            return true;
        }
        if (loginUser.getPermissions() != null && loginUser.getPermissions().contains(ADMIN_ROLE_KEY)) {
            return true;
        }
        // 兜底：检查用户角色的 roleKey
        if (user.getRoles() != null) {
            for (com.pnkx.common.core.domain.entity.SysRole role : user.getRoles()) {
                if (ADMIN_ROLE_KEY.equals(role.getRoleKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否存在注解，如果存在就获取
     */
    private DataScopeSelf getAnnotation(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        if (method != null) {
            return method.getAnnotation(DataScopeSelf.class);
        }
        return null;
    }
}
