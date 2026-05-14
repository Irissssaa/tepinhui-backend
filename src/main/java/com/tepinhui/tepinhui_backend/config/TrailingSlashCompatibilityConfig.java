package com.tepinhui.tepinhui_backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration
public class TrailingSlashCompatibilityConfig {

    @Bean
    public FilterRegistrationBean<UrlHandlerFilter> trailingSlashCompatibilityFilter() {
        UrlHandlerFilter filter = UrlHandlerFilter
            .trailingSlashHandler("/api/v1/**", "/auth/**")
            .wrapRequest()
            .build();

        FilterRegistrationBean<UrlHandlerFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
