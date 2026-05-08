package com.tepinhui.tepinhui_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("特品汇 API")
                        .version("1.0.0")
                        .description("特品汇后端接口文档")
                        .contact(new Contact().name("Tepinhui Team")));
    }
}
