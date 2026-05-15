package com.tepinhui.tepinhui_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.avatar-dir:./uploads/avatar/}")
    private String avatarDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/avatar/** 请求映射到本地 avatarDir 目录
        String resourceLocation = "file:" + (avatarDir.endsWith("/") ? avatarDir : avatarDir + "/");
        registry.addResourceHandler("/uploads/avatar/**")
                .addResourceLocations(resourceLocation);
    }
}
