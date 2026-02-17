package com.ecommerce.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.dto.OrderRequestDTO;
import com.ecommerce.order.dto.OrderResponseDTO;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    Long userId = 1L; //Todo remove it and extract from authentication principal
    
    /**
     * USER API: Place a new order
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Place a new order",
            description = "Place a new order with items and payment status. User role required.")
    @Tag(name = "User")
    public ResponseEntity<OrderResponseDTO> placeOrder(
            @Valid @RequestBody OrderRequestDTO orderRequestDTO,
            Authentication authentication) {
        log.info("POST /api/orders - Placing order");

        Long userId = extractUserId(authentication);
        OrderResponseDTO response = orderService.placeOrder(userId, orderRequestDTO);
        if (response.getStatus() == OrderStatus.PAID) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
    }

    /**
     * USER API: Get my orders
     */
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get my orders",
            description = "Retrieve all orders for the current user. User role required.")
    @Tag(name = "User")
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders(
            Authentication authentication) {
        log.info("GET /api/orders/my-orders - Fetching user orders");

        Long userId = extractUserId(authentication);
        List<OrderResponseDTO> response = orderService.getUserOrders(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN API: Get all orders
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders",
            description = "Retrieve all orders in the system. Admin role required.")
    @Tag(name = "Admin")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        log.info("GET /api/orders - Fetching all orders");

        List<OrderResponseDTO> response = orderService.getAllOrders();
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN API: Cancel order
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel order",
            description = "Cancel an existing order and release reserved stock. Admin role required.")
    @Tag(name = "Admin")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @PathVariable Long id) {
        log.info("PUT /api/orders/{}/cancel - Cancelling order", id);

        OrderResponseDTO response = orderService.cancelOrder(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Extract userId from Authentication principal
     * Principal format: userId (set by RoleExtractionFilter)
     */
    private Long extractUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            try {
                return Long.parseLong((String) authentication.getPrincipal());
            } catch (NumberFormatException e) {
                log.warn("Could not parse user ID from principal");
                return null;
            }
        }
        return null;
    }
}
