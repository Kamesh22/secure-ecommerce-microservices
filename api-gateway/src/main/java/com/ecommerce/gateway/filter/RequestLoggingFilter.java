package com.ecommerce.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        var request = exchange.getRequest();

        log.info("Incoming Request -> Method: {} | URI: {} | Headers: {}",
                request.getMethod(),
                request.getURI(),
                request.getHeaders());

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    var response = exchange.getResponse();
                    log.info("Outgoing Response -> Status: {}", response.getStatusCode());
                }));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}