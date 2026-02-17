package com.ecommerce.gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authFallback() {

        Map<String, Object> body = Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 503,
                "error", "Service Unavailable",
                "message", "Auth Service is currently unavailable. Please try again later.",
                "path", "/api/v1/auth/**"
        );

        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
        );
    }
}