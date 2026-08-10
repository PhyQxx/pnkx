package com.pnkx.framework.interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.utils.StringUtils;

/**
 * 基于 ThreadLocal 的分页拦截器（PageHelper 风格），以 MP InnerInterceptor 形式注册。
 * <p>
 * 替代已移除的 PageHelper：
 * <ul>
 *   <li>Controller 调用 {@link BaseController#startPage()} 后，分页信息存入 ThreadLocal；</li>
 *   <li>本拦截器在 beforeQuery 阶段读取 ThreadLocal 的 Page，
 *       执行 count 查询回填 total，并改写 BoundSql 追加 {@code LIMIT offset, size}；</li>
 *   <li>{@link BaseController#getDataTable(java.util.List)} 从 ThreadLocal 取出 total 组装响应。</li>
 * </ul>
 * 不依赖 MP 原生分页（IPage 参数注入），Mapper 方法签名保持若依原生的单参数
 * {@code List<T> selectXxxList(T entity)}，XML 中的 {@code #{field}} 引用无需改动。
 *
 * @author phy
 */
public class PageInterceptor implements InnerInterceptor {
    private static final Log logger = LogFactory.getLog(PageInterceptor.class);

    /**
     * count 查询的 msId 后缀
     */
    private static final String COUNT_MS_SUFFIX = "_pnkx_count";

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) {
        IPage<?> page = BaseController.getPage();
        // 无分页上下文，或不分页（size <= 0），直接放行
        if (page == null || page.getSize() <= 0) {
            return;
        }
        // 只拦截 SELECT
        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
            BaseController.clearPage();
            return;
        }

        String originalSql = boundSql.getSql();
        try {
            // 1. 执行 count 查询，回填 total（通过重建 count MappedStatement + Executor 复用同事务连接）
            long total = executeCount(executor, ms, boundSql, originalSql, parameter, rowBounds, resultHandler);
            page.setTotal(total);

            // 2. 改写 BoundSql 追加 LIMIT（反射更新 sql 字段）
            long offset = (page.getCurrent() - 1) * page.getSize();
            String pagedSql = originalSql + " LIMIT " + offset + ", " + page.getSize();
            org.apache.ibatis.reflection.MetaObject metaObject =
                    org.apache.ibatis.reflection.SystemMetaObject.forObject(boundSql);
            metaObject.setValue("sql", pagedSql);
        } catch (Exception e) {
            logger.error("分页拦截器执行失败: " + e.getMessage(), e);
            BaseController.clearPage();
        }
    }

    /**
     * 执行 count 查询获取总记录数
     * <p>
     * 策略：去掉原 SQL 末尾的 ORDER BY，外包一层 {@code SELECT COUNT(*) FROM (...) tmp}，
     * 构造 count 专用的 MappedStatement，复用原 Executor（同事务连接）执行。
     */
    @SuppressWarnings("rawtypes")
    private long executeCount(Executor executor, MappedStatement ms, BoundSql boundSql, String originalSql,
                              Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws Exception {
        String countSql = "SELECT COUNT(*) FROM (" + stripOrderBy(originalSql) + ") _pnkx_count_table_";

        Configuration configuration = ms.getConfiguration();
        List<ParameterMapping> countMappings = new ArrayList<>(boundSql.getParameterMappings());
        BoundSql countBoundSql = new BoundSql(configuration, countSql, countMappings, boundSql.getParameterObject());
        // 复制 additionalParameter（MyBatis 动态参数，如 <bind> 产生的）
        for (ParameterMapping pm : boundSql.getParameterMappings()) {
            String prop = pm.getProperty();
            if (boundSql.hasAdditionalParameter(prop)) {
                countBoundSql.setAdditionalParameter(prop, boundSql.getAdditionalParameter(prop));
            }
        }

        // 重建一个 count 专用的 MappedStatement（基于原 ms，换 id 和 sqlSource）
        MappedStatement countMs = buildCountMappedStatement(ms, configuration, countBoundSql);

        // 通过 Executor 执行 count（复用同事务的连接，参数一致）
        List<?> rows = executor.query(countMs, parameter, rowBounds, resultHandler, null, countBoundSql);
        if (rows == null || rows.isEmpty()) {
            return 0L;
        }
        Object countVal = rows.get(0);
        if (countVal == null) {
            return 0L;
        }
        // COUNT(*) 返回 Long 或 BigInteger（视驱动），统一转 long
        if (countVal instanceof Number) {
            return ((Number) countVal).longValue();
        }
        return Long.parseLong(countVal.toString());
    }

    /**
     * 基于 MappedStatement 的 resultType 构造 count 查询用的 MappedStatement
     */
    private MappedStatement buildCountMappedStatement(MappedStatement ms, Configuration configuration, BoundSql countBoundSql) {
        SqlSource countSqlSource = new org.apache.ibatis.builder.StaticSqlSource(configuration, countBoundSql.getSql(), countBoundSql.getParameterMappings());
        MappedStatement.Builder builder = new MappedStatement.Builder(
                configuration, ms.getId() + COUNT_MS_SUFFIX, countSqlSource, SqlCommandType.SELECT);
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(StatementType.PREPARED);
        builder.timeout(ms.getTimeout());
        // count 结果直接用 Long 接收（SELECT COUNT(*) FROM ... 返回单列单行）
        builder.resultMaps(Collections.singletonList(
                new ResultMap.Builder(configuration, ms.getId() + COUNT_MS_SUFFIX + "-Inline",
                        Long.class, Collections.emptyList(), null).build()
        ));
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.keyGenerator(org.apache.ibatis.executor.keygen.NoKeyGenerator.INSTANCE);
        builder.lang(ms.getLang());
        return builder.build();
    }

    /**
     * 去掉 SQL 末尾的 ORDER BY 子句（用于 count 优化）
     */
    private String stripOrderBy(String sql) {
        if (StringUtils.isEmpty(sql)) {
            return sql;
        }
        String upper = sql.toUpperCase();
        int orderByIndex = upper.lastIndexOf(" ORDER BY ");
        if (orderByIndex > 0) {
            String afterOrderBy = sql.substring(orderByIndex);
            // 仅当 ORDER BY 不在子查询内（后面无右括号）时才去除
            if (!afterOrderBy.contains(")")) {
                return sql.substring(0, orderByIndex);
            }
        }
        return sql;
    }
}
