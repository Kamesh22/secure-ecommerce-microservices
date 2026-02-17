package com.ecommerce.gateway.filter;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private final RouteValidator routeValidator;
    private final WebClient.Builder webClientBuilder;

    private static final String AUTH_VALIDATE_URL = "http://auth-service/api/auth/validate";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        if (!routeValidator.isSecured.test(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
        }

        return webClientBuilder.build()
                .get()
                .uri(AUTH_VALIDATE_URL)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(TokenValidationResponse.class)
                .flatMap(response -> {

                    if (!response.isValid()) {
                        return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
                    }

                    var mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", String.valueOf(response.getUserId()))
                            .header("X-User-Roles", response.getRole())
                            .header("X-Username", response.getUsername())
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(ex -> {
                    log.error("Auth service error: {}", ex.getMessage());
                    return onError(exchange, "Auth service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Data
    public static class TokenValidationResponse {
        private Long userId;
        private String username;
        private String role;
        private boolean valid;
    }
}