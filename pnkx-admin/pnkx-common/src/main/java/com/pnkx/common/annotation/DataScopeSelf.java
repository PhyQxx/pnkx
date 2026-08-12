package com.pnkx.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 个人数据权限过滤注解。
 * <p>
 * 标注在 Service 的列表/聚合查询方法上，实现「默认只能看自己创建的，
 * 同群组成员数据互见，管理员无限制」的数据权限。
 * <p>
 * 规则（命中其一即不限数据）：
 * <ol>
 *   <li>超级管理员（userId = 1）</li>
 *   <li>拥有 admin 角色标识的用户（通过 LoginUser.getPermissions() 判断）</li>
 * </ol>
 * 否则按「自己 + 所在群组成员」的 userId 集合过滤 create_by。
 * <p>
 * 用法：方法第一个参数需为 {@link com.pnkx.common.core.domain.BaseEntity} 的子类，
 * 切面会把可见 userId 集合写入其 params（key = {@link #SCOPE_USER_IDS}），
 * Mapper XML 中据此拼接 `AND create_by IN (...)`。
 *
 * @author pnkx
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScopeSelf {

    /**
     * create_by 字段所属表的别名。
     * 例如查询为 {@code FROM px_bookkeeping_record r}，则填 "r"。
     * 单表查询且无别名时填空字符串即可，XML 中直接用 create_by。
     */
    String alias() default "";

    /**
     * 是否严格只过滤当前登录用户本人（忽略群组共享与管理员全量权限）。
     * <p>
     * 为 true 时，切面只注入当前用户自己的 userId，既不扩展到所在群组成员，
     * 也不因管理员身份而放开。适用于「最近使用」这类高度个性化、
     * 只应反映用户本人使用习惯的聚合查询。
     */
    boolean onlySelf() default false;

    /**
     * params 中存放可见 userId 集合的 key。
     */
    String SCOPE_USER_IDS = "scopeUserIds";

    /**
     * params 中标识「是否不限数据权限」的 key（true 时 XML 应跳过过滤）。
     */
    String SCOPE_ALL = "scopeAll";
}
