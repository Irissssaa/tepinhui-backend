package com.tepinhui.tepinhui_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("特品汇 API")
                        .version("1.0.0")
                        .description("特品汇后端接口文档\n\n" +
                                "**使用方法：**\n" +
                                "1. 先调用 `/auth/login` 接口获取 JWT Token\n" +
                                "2. 点击下方 **Authorize** 按钮\n" +
                                "3. 在输入框中粘贴 Token（不需要带 `Bearer ` 前缀）\n" +
                                "4. 点击 **Authorize** 确认\n" +
                                "5. 之后所有需要认证的接口都会自动带上 Token")
                        .contact(new Contact().name("Tepinhui Team")))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("输入从 `/auth/login` 获取的 JWT Token")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));

        return openAPI;
    }
}
