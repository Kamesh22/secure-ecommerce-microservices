package com.ecommerce.gateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    private static final List<String> OPEN_ENDPOINTS = List.of(

            // AUTH
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/validate",

            // ACTUATOR
            "/actuator",
            "/actuator/**"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> OPEN_ENDPOINTS.stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
}