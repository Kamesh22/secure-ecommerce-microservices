package com.ecommerce.order.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.client.InventoryServiceClient;
import com.ecommerce.order.dto.OrderItemDTO;
import com.ecommerce.order.dto.OrderRequestDTO;
import com.ecommerce.order.dto.OrderResponseDTO;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final InventoryServiceClient inventoryServiceClient;

    @Override
    public OrderResponseDTO placeOrder(Long userId, OrderRequestDTO orderRequestDTO) {
        log.info("Placing order for user ID: {}", userId);

        // Step 1: Create order with status = CREATED
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CREATED)
                .items(new ArrayList<>())
                .paymentSuccess(orderRequestDTO.getPaymentSuccess())
                .build();

        // Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemDTO itemDTO : orderRequestDTO.getItems()) {
            // Step 2: Call inventory service to reserve stock
            try {
                inventoryServiceClient.reserveStock(itemDTO.getProductId(), itemDTO.getQuantity());
                log.info("Stock reserved for product ID: {}, quantity: {}", itemDTO.getProductId(), itemDTO.getQuantity());
            } catch (Exception e) {
                log.error("Failed to reserve stock for product ID: {}", itemDTO.getProductId());
                throw new BusinessException("Failed to reserve stock for product: " + itemDTO.getProductId());
            }

            // Create order item
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemDTO.getProductId())
                    .quantity(itemDTO.getQuantity())
                    .price(itemDTO.getPrice())
                    .build();
            orderItems.add(orderItem);

            // Calculate item total
            totalAmount = totalAmount.add(itemDTO.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
        }

        // Step 3: Update order status to RESERVED
        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.RESERVED);

        // Step 4: Check payment success flag
        if (orderRequestDTO.getPaymentSuccess()) {
            // Call inventory confirm for each item
            for (OrderItemDTO itemDTO : orderRequestDTO.getItems()) {
                try {
                    inventoryServiceClient.confirmStock(itemDTO.getProductId(), itemDTO.getQuantity());
                    log.info("Stock confirmed for product ID: {}, quantity: {}", itemDTO.getProductId(), itemDTO.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to confirm stock for product ID: {}", itemDTO.getProductId());
                    // Release already reserved stock
                    releaseReservedStockForOrder(orderRequestDTO.getItems());
                    throw new BusinessException("Failed to confirm stock for product: " + itemDTO.getProductId());
                }
            }
            order.setStatus(OrderStatus.PAID);
        } else {
            // Call inventory release for each item
            try {
                releaseReservedStockForOrder(orderRequestDTO.getItems());
            } catch (Exception e) {
                log.error("Failed to release reserved stock");
                throw new BusinessException("Failed to process order payment failure");
            }
            order.setStatus(OrderStatus.FAILED);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}, status: {}", savedOrder.getId(), savedOrder.getStatus());

        return orderMapper.orderToOrderResponseDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId) {
        log.info("Fetching order with ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        return orderMapper.orderToOrderResponseDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getUserOrders(Long userId) {
        log.info("Fetching orders for user ID: {}", userId);

        return orderRepository.findByUserId(userId)
                .stream()
                .map(orderMapper::orderToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        log.info("Fetching all orders");

        return orderRepository.findAll()
                .stream()
                .map(orderMapper::orderToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponseDTO cancelOrder(Long orderId) {
        log.info("Cancelling order with ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() == OrderStatus.RESERVED) {
            // Release reserved stock
            for (OrderItem item : order.getItems()) {
                try {
                    inventoryServiceClient.releaseStock(item.getProductId(), item.getQuantity());
                    log.info("Stock released for product ID: {}, quantity: {}", item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to release stock for product ID: {}", item.getProductId());
                    throw new BusinessException("Failed to release stock for product: " + item.getProductId());
                }
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);
        log.info("Order cancelled with ID: {}", orderId);

        return orderMapper.orderToOrderResponseDTO(updatedOrder);
    }

    /**
     * Helper method to release reserved stock
     */
    private void releaseReservedStockForOrder(List<OrderItemDTO> items) {
        for (OrderItemDTO itemDTO : items) {
            try {
                inventoryServiceClient.releaseStock(itemDTO.getProductId(), itemDTO.getQuantity());
                log.info("Stock released for product ID: {}, quantity: {}", itemDTO.getProductId(), itemDTO.getQuantity());
            } catch (Exception e) {
                log.error("Failed to release stock for product ID: {}", itemDTO.getProductId());
                throw new BusinessException("Failed to release stock for product: " + itemDTO.getProductId());
            }
        }
    }
}
