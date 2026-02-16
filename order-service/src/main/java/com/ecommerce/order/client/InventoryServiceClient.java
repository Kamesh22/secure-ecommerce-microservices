package com.ecommerce.order.client;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.order.client.dto.InventoryConfirmRequest;
import com.ecommerce.order.client.dto.InventoryReleaseRequest;
import com.ecommerce.order.client.dto.InventoryReserveRequest;
import com.ecommerce.order.client.dto.InventoryResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryServiceClient {

    private final WebClient.Builder webClientBuilder;  

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * Reserve stock
     */
    public InventoryResponse reserveStock(Long productId, Integer quantity) {
        InventoryReserveRequest request = InventoryReserveRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();

        return callInventoryApi("/api/inventory/reserve", request, "reserve", productId);
    }

    /**
     * Release stock
     */
    public InventoryResponse releaseStock(Long productId, Integer quantity) {
        InventoryReleaseRequest request = InventoryReleaseRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();

        return callInventoryApi("/api/inventory/release", request, "release", productId);
    }

    /**
     * Confirm stock
     */
    public InventoryResponse confirmStock(Long productId, Integer quantity) {
        InventoryConfirmRequest request = InventoryConfirmRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();

        return callInventoryApi("/api/inventory/confirm", request, "confirm", productId);
    }

    /**
     * Common WebClient executor
     */
    private InventoryResponse callInventoryApi(String path, Object body, String action, Long productId) {

        log.info("Calling Inventory Service to {} stock for productId={}", action, productId);

        return webClientBuilder.build()
                .post()
                .uri("lb://inventory-service" + path) 
                .header("X-Internal-Call", "ORDER_SERVICE")
                .header("X-User-Roles", "INTERNAL")
                .bodyValue(body)
                .retrieve()
                //handle HTTP errors
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Inventory service error: {}", errorBody);
                                    return Mono.error(new BusinessException(
                                            "Inventory service failed during " + action +
                                                    " for product " + productId));
                                })
                )
                .bodyToMono(InventoryResponse.class)
                .timeout(TIMEOUT)
                .doOnError(error ->
                        log.error("Error during {} stock for productId={}: {}",
                                action, productId, error.getMessage())
                )
                .block();
    }
}