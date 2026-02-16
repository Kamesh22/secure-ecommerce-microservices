package com.ecommerce.order.client;

import com.ecommerce.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductServiceClient {

    private final WebClient.Builder webClientBuilder;

    public ProductResponse getProduct(Long productId) {

        log.info("Fetching product details for productId={}", productId);

        return webClientBuilder.build()
                .get()
                .uri("lb://product-service/api/products/{id}", productId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(error -> {
                                    log.error("Product service error: {}", error);
                                    return Mono.error(new BusinessException("Failed to fetch product " + productId));
                                })
                )
                .bodyToMono(ProductResponse.class)
                .block();
    }
}