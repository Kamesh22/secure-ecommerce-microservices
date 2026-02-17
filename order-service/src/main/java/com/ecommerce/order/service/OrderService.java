package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderRequestDTO;
import com.ecommerce.order.dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {

    /**
     * Place a new order
     * Handles stock reservation and payment processing
     */
    OrderResponseDTO placeOrder(Long userId, OrderRequestDTO orderRequestDTO);

    /**
     * Get all orders for a user
     */
    List<OrderResponseDTO> getUserOrders(Long userId);

    /**
     * Get all orders (ADMIN only)
     */
    List<OrderResponseDTO> getAllOrders();

    /**
     * Cancel an order (ADMIN only)
     */
    OrderResponseDTO cancelOrder(Long orderId);
}
