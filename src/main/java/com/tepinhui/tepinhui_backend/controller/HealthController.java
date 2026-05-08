package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    @Value("${spring.application.name:application}")
    private String applicationName;

    @Value("${app.api-prefix:/tph}")
    private String apiPrefix;

    @Value("${app.health-endpoint:/health}")
    private String healthEndpoint;

    @Value("${server.port:8060}")
    private int serverPort;

    private Instant startupTime;

    @PostConstruct
    public void init() {
        startupTime = Instant.now();
    }

    @GetMapping("${app.health-endpoint:/health}")
    public Result<Map<String, Object>> health() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "UP");
        payload.put("application", applicationName);
        payload.put("timestamp", formatToChinaTime(Instant.now()));
        payload.put("startup-time", formatToChinaTime(startupTime));
        payload.put("branch", getGitBranch());
        payload.put("port", serverPort);
        payload.put("context-path", apiPrefix);
        payload.put("health-path", apiPrefix + healthEndpoint);
        return Result.success(payload);
    }

    private String formatToChinaTime(Instant instant) {
        ZonedDateTime zonedDateTime = instant.atZone(CHINA_ZONE);
        return zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z"));
    }

    private String getGitBranch() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"git", "rev-parse", "--abbrev-ref", "HEAD"});
            process.waitFor();
            byte[] bytes = process.getInputStream().readAllBytes();
            return new String(bytes).trim();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
