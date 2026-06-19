package com.example.demomessage.controller;

import com.example.demomessage.config.DemoProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DemoController {

    private final DemoProperties demoProperties;

    public DemoController(DemoProperties demoProperties) {
        this.demoProperties = demoProperties;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "service", demoProperties.getServiceName()
        );
    }

    @GetMapping("/version")
    public Map<String, String> version() {
        return Map.of("version", demoProperties.getVersion());
    }

    @GetMapping("/message")
    public Map<String, String> message() {
        return currentDemoConfig();
    }

    @GetMapping("/config")
    public Map<String, String> config() {
        return currentDemoConfig();
    }

    private Map<String, String> currentDemoConfig() {
        return Map.of(
                "service", demoProperties.getServiceName(),
                "version", demoProperties.getVersion(),
                "environment", demoProperties.getEnvironment(),
                "message", demoProperties.getMessage()
        );
    }
}
