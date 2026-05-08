package com.tepinhui.tepinhui_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${app.api-prefix:/tph}")
    private String apiPrefix;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("特品汇 API")
                        .version("1.0.0")
                        .description("特品汇后端接口文档\n\n" +
                                "**使用方法：**\n" +
                                "1. 先调用登录接口获取 JWT Token\n" +
                                "2. 点击下方 **Authorize** 按钮\n" +
                                "3. 在输入框中粘贴 Token（不需要带 `Bearer ` 前缀）\n" +
                                "4. 点击 **Authorize** 确认\n" +
                                "5. 之后所有需要认证的接口都会自动带上 Token\n\n" +
                                "**路径说明：**\n" +
                                "- 所有接口前缀为 `" + apiPrefix + "`\n" +
                                "- 认证接口: `" + apiPrefix + "/auth/*`\n" +
                                "- 业务接口: `" + apiPrefix + "/api/v1/*`\n" +
                                "- 测试时直接使用页面显示的完整路径即可")
                        .contact(new Contact().name("Tepinhui Team")))
                .servers(List.of(
                        new Server()
                                .url(apiPrefix)
                                .description("服务地址")
                ))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("输入从登录接口获取的 JWT Token")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));

        return openAPI;
    }
}
