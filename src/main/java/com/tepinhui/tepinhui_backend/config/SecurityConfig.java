package com.tepinhui.tepinhui_backend.config;

import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 健康检查端点路径（相对于 context-path）
    // 例如: 当 context-path=/tph 时，实际路径为 /tph/health
    @Value("${app.health-endpoint:/health}")
    private String healthEndpoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/login",      // 用户登录
                    "/auth/register/code", // 邮箱获取注册验证码
                    "/auth/register",   // 用户注册
                    "/auth/refresh",    // 刷新 Token
                    healthEndpoint,     // 健康检查（相对于 context-path）
                    "/swagger-ui/**",   // Swagger UI
                    "/swagger-ui.html", // Swagger 入口
                    "/swagger-ui/index.html", // Swagger index
                    "/v3/api-docs/**",  // SpringDoc 3.x API 文档
                    "/api-docs/**",     // SpringDoc 2.x API 文档
                    "/swagger-config/**"// Swagger 配置
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/trace/{traceCode}", "/api/v1/trace/product/{productId}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/map/specialties", "/api/v1/specialties/{id}", "/api/v1/map/season-recommend").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/products").hasAnyRole("ADMIN", "MERCHANT")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/*").hasAnyRole("ADMIN", "MERCHANT")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/*").hasAnyRole("ADMIN", "MERCHANT")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/merchant/**").hasAnyRole("ADMIN", "MERCHANT")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173", "http://82.156.12.252", "https://82.156.12.252"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
