package com.pnkx.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.pnkx.common.config.PnkxConfig;

/**
 * SpringDoc OpenAPI 接口配置
 *
 * @author phy
 */
@Configuration
public class SwaggerConfig {
    /**
     * 系统基础配置
     */
    @Autowired
    private PnkxConfig pnkxConfig;

    /**
     * 是否开启swagger
     */
    @Value("${swagger.enabled:true}")
    private boolean enabled;

    /**
     * 创建API
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Authorization";
        return new OpenAPI()
                .info(new Info()
                        .title("Pei你看雪博客管理_接口文档")
                        .description("用于管理Pei你看雪博客")
                        .version(pnkxConfig.getVersion())
                        .contact(new Contact().name(pnkxConfig.getName())))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
