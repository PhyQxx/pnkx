package com.pnkx.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.pnkx.framework.interceptor.PageInterceptor;

/**
 * MyBatis-Plus 配置
 *
 * @author phy
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器链
     * <ul>
     *   <li>{@link PageInterceptor}：基于 ThreadLocal 的分页拦截器（PageHelper 风格），
     *       配合 {@link com.pnkx.common.core.controller.BaseController#startPage()} 使用，
     *       在 beforeQuery 阶段执行 count 并改写 SQL 追加 LIMIT。</li>
     *   <li>{@link PaginationInnerInterceptor}：MP 原生分页插件（保留备用，
     *       对 Mapper 方法参数中携带 {@code IPage} 的查询生效；现有代码不依赖它）。</li>
     * </ul>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // ThreadLocal 分页拦截器（若依风格，对单参数 selectXxxList 生效）
        interceptor.addInnerInterceptor(new PageInterceptor());
        return interceptor;
    }
}
