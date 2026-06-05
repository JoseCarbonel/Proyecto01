package com.javaresources.controller;

import com.javaresources.security.SecurityService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/security")
public class SecurityController {

    private final SecurityService svc;
    public SecurityController(SecurityService svc) { this.svc = svc; }

    @PostMapping("/hash")
    public Map<String, Object> hash(@RequestBody Map<String, String> body) {
        return svc.hashDemo(body.getOrDefault("input", "test"));
    }

    @PostMapping("/hmac")
    public Map<String, Object> hmac(@RequestBody Map<String, String> body) {
        return svc.hmacDemo(body.getOrDefault("data", "test-data"));
    }

    @PostMapping("/base64")
    public Map<String, Object> base64(@RequestBody Map<String, String> body) {
        return svc.base64Demo(body.getOrDefault("input", "hola mundo"));
    }

    @GetMapping("/token")
    public Map<String, Object> token(@RequestParam(defaultValue = "32") int length) {
        return svc.tokenDemo(length);
    }

    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestBody Map<String, String> body) {
        return svc.validateDemo(
            body.getOrDefault("email", ""),
            body.getOrDefault("input", "")
        );
    }
}
