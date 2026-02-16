package com.ecommerce.order.client;

import com.ecommerce.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryServiceClient {

    private final WebClient webClient;

    @Value("${app.inventory-service.url:http://localhost:8082}")
    private String inventoryServiceUrl;

    /**
     * Reserve stock for order items
     */
    public InventoryResponse reserveStock(Long productId, Integer quantity) {
        log.info("Reserving {} units for product ID: {}", quantity, productId);

        InventoryReserveRequest request = InventoryReserveRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();

        try {
            return webClient
                    .post()
                    .uri(inventoryServiceUrl + "/api/inventory/reserve")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(InventoryResponse.class)
                    .doOnError(error -> log.error("Error reserving stock: {}", error.getMessage()))
                    .block();
        } catch (Exception e) {
            log.error("Failed to reserve stock for product ID: {}", productId, e);
            throw new BusinessException("Failed to reserve stock for product ID: " + productId);
        }
    }

    /**
     * Release reserved stock
     */
    public InventoryResponse releaseStock(Long productId, Integer quantity) {
        log.info("Releasing {} units for product ID: {}", quantity, productId);

        InventoryReleaseRequest request = InventoryReleaseRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();

        try {
            return webClient
                    .post()
                    .uri(inventoryServiceUrl + "/api/inventory/release")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(InventoryResponse.class)
                    .doOnError(error -> log.error("Error releasing stock: {}", error.getMessage()))
                    .block();
        } catch (Exception e) {
            log.error("Failed to release stock for product ID: {}", productId, e);
            throw new BusinessException("Failed to release stock for product ID: " + productId);
        }
    }

    /**
     * Confirm reserved stock (permanent deduction)
     */
    public InventoryResponse confirmStock(Long productId, Integer quantity) {
        log.info("Confirming {} units for product ID: {}", quantity, productId);

        InventoryConfirmRequest request = InventoryConfirmRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .build();

        try {
            return webClient
                    .post()
                    .uri(inventoryServiceUrl + "/api/inventory/confirm")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(InventoryResponse.class)
                    .doOnError(error -> log.error("Error confirming stock: {}", error.getMessage()))
                    .block();
        } catch (Exception e) {
            log.error("Failed to confirm stock for product ID: {}", productId, e);
            throw new BusinessException("Failed to confirm stock for product ID: " + productId);
        }
    }
}
