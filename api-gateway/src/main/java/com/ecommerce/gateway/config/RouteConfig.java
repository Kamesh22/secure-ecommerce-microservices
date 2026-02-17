package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    private static final String PREFIX = "/api/v1";

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {

        return builder.routes()

                // PRODUCT SERVICE
                .route("product-service", r -> r
                        .path(PREFIX + "/products/**")
                        .filters(f -> f.rewritePath(PREFIX + "/(?<segment>.*)", "/api/${segment}"))
                        .uri("lb://product-service")
                )

                // INVENTORY SERVICE
                .route("inventory-service", r -> r
                        .path(PREFIX + "/inventory/**")
                        .filters(f -> f.rewritePath(PREFIX + "/(?<segment>.*)", "/api/${segment}"))
                        .uri("lb://inventory-service")
                )

                // ORDER SERVICE
                .route("order-service", r -> r
                        .path(PREFIX + "/orders/**")
                        .filters(f -> f.rewritePath(PREFIX + "/(?<segment>.*)", "/api/${segment}"))
                        .uri("lb://order-service")
                )

                // AUTH SERVICE
                .route("auth-service", r -> r
                        .path(PREFIX + "/auth/**")
                        .filters(f -> f.rewritePath(PREFIX + "/(?<segment>.*)", "/api/${segment}"))
                        .uri("lb://auth-service")
                )
                .build();
    }
}