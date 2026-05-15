package com.tepinhui.tepinhui_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.health-endpoint:/health}")
    private String healthEndpoint;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationEntryPoint authenticationEntryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/login",
                    "/auth/register/code",
                    "/auth/register",
                    "/auth/refresh",
                    healthEndpoint,
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-ui/index.html",
                    "/v3/api-docs/**",
                    "/api-docs/**",
                    "/swagger-config/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/products").hasAnyRole("MERCHANT", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/specialties", "/api/v1/specialties/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/map/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/stats/*", "/api/v1/stats/*/*").permitAll()
                .requestMatchers("/api/v1/trace/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/trace/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/trace/product/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/trace").hasAnyRole("ADMIN", "MERCHANT")
                .requestMatchers("/api/v1/user/**").hasAnyRole("ADMIN", "CONSUMER", "MERCHANT")
                .requestMatchers("/api/v1/addresses/**").hasAnyRole("ADMIN", "CONSUMER")
                .requestMatchers("/api/v1/cart/**").hasAnyRole("ADMIN", "CONSUMER")
                .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasAnyRole("ADMIN", "CONSUMER")
                .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasAnyRole("ADMIN", "CONSUMER")
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/*").hasAnyRole("ADMIN", "CONSUMER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*/cancel").hasAnyRole("ADMIN", "CONSUMER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*/confirm").hasAnyRole("ADMIN", "CONSUMER")
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/*/review").hasAnyRole("ADMIN", "CONSUMER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/orders/*/ship").hasAnyRole("ADMIN", "MERCHANT")
                .requestMatchers(HttpMethod.POST, "/api/v1/merchant/apply").hasAnyRole("CONSUMER", "MERCHANT", "ADMIN")
                .requestMatchers("/api/v1/merchant/**").hasAnyRole("ADMIN", "MERCHANT")
                .requestMatchers("/api/v1/admin/specialties/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/merchant/**").hasAnyRole("ADMIN", "MERCHANT")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) ->
            writeJson(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, Result.error(401, "未登录"));
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, accessDeniedException) ->
            writeJson(response, objectMapper, HttpServletResponse.SC_FORBIDDEN, Result.error(403, "权限不足"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = java.util.Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeJson(HttpServletResponse response,
                           ObjectMapper objectMapper,
                           int status,
                           Result<Void> body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
