package com.freightfox.transporter.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> getApiInfo() {
        return Map.of(
                "service", "FreightFox Transporter Assignment API",
                "message", "API is running.",
                "documentation", "/swagger-ui/index.html",
                "health", "/actuator/health",
                "endpoints", Map.of(
                        "submitInput", "POST /api/v1/transporters/input",
                        "assignment", "POST /api/v1/transporters/assignment"
                )
        );
    }
}