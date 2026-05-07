package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${spring.application.name:application}")
    private String applicationName;

    @Value("${app.api-prefix:/tph}")
    private String apiPrefix;

    @Value("${app.health-endpoint:/health}")
    private String healthEndpoint;

    @Value("${server.port:8060}")
    private int serverPort;

    @GetMapping("${app.health-endpoint:/health}")
    public Result<Map<String, Object>> health() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "UP");
        payload.put("application", applicationName);
        payload.put("timestamp", Instant.now().toString());
        payload.put("port", serverPort);
        payload.put("context-path", apiPrefix);
        payload.put("health-path", apiPrefix + healthEndpoint);
        return Result.success(payload);
    }
}
