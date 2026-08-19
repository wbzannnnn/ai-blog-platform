package com.lanou.springaidemo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI 配置类
 * 配置 JWT 认证支持，使 Swagger UI 显示 Authorize 按钮
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI智能博客系统 API")
                        .version("1.0.0")
                        .description("AI智能博客系统后端接口文档")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@example.com")))
                // 添加安全要求
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                // 配置 JWT 认证方案
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("请输入登录后获取的 JWT Token，格式为：Bearer <token>")));
    }
}
