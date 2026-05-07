package com.Application.SocietyManagement.users.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Society Management API is running");
    }
}
