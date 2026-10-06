package com.itsm.boot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 文档(springdoc):/swagger-ui.html */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI itsmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ITSM Platform API")
                        .version("v1")
                        .description("ITSM 一体化 IT 服务管理平台 OpenAPI(API 清单见 docs/产品设计文档.md 第 9 章)"))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

