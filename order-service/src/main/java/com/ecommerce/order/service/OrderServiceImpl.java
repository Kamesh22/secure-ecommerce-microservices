package com.ecommerce.order.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.client.InventoryServiceClient;
import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.client.dto.InventoryResponse;
import com.ecommerce.order.client.dto.ProductResponse;
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
    private final ProductServiceClient productServiceClient;

    @Override
    public OrderResponseDTO placeOrder(Long userId, OrderRequestDTO orderRequestDTO) {

        if (orderRequestDTO.getItems() == null || orderRequestDTO.getItems().isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }

        log.info("Placing order for userId={}", userId);

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CREATED)
                .items(new ArrayList<>())
                .paymentSuccess(orderRequestDTO.getPaymentSuccess())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderItemDTO> reservedItems = new ArrayList<>();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemDTO itemDTO : orderRequestDTO.getItems()) {

            ProductResponse product = productServiceClient.getProduct(itemDTO.getProductId());
            if (product == null) {
                throw new BusinessException("Product not found: " + itemDTO.getProductId());
            }
            BigDecimal price = product.getPrice();

            // reserve stock
            try {
                InventoryResponse response = inventoryServiceClient.reserveStock(itemDTO.getProductId(), itemDTO.getQuantity());
                if (response == null) {
                    releaseReservedStockForOrder(reservedItems);
                    throw new BusinessException("Failed to reserve stock for product: " + itemDTO.getProductId());
                }
                reservedItems.add(itemDTO); // track reserved items for potential rollback
            } catch (Exception e) {
                releaseReservedStockForOrder(reservedItems); // rollback previous reservations
                throw new BusinessException("Failed to reserve stock for product: " + itemDTO.getProductId());
            }
            OrderItem orderItem = OrderItem.builder()
                    .productId(itemDTO.getProductId())
                    .quantity(itemDTO.getQuantity())
                    .price(price)
                    .order(order)
                    .build();

            totalAmount = totalAmount.add(
                    price.multiply(BigDecimal.valueOf(itemDTO.getQuantity()))
            );
            orderItems.add(orderItem);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.RESERVED);

        // Payment flow
        if (Boolean.TRUE.equals(orderRequestDTO.getPaymentSuccess())) {

            for (OrderItemDTO itemDTO : orderRequestDTO.getItems()) {
                try {
                    inventoryServiceClient.confirmStock(itemDTO.getProductId(), itemDTO.getQuantity());
                    log.info("Confirmed stock for productId={} qty={}", itemDTO.getProductId(), itemDTO.getQuantity());
                } catch (Exception ex) {
                    log.error("Confirm failed for productId={}", itemDTO.getProductId());
                    releaseReservedStockForOrder(reservedItems);
                    throw new BusinessException("Failed to confirm stock for product: " + itemDTO.getProductId());
                }
            }

            order.setStatus(OrderStatus.PAID);

        } else {

            releaseReservedStockForOrder(reservedItems);
            order.setStatus(OrderStatus.FAILED);
        }

        Order savedOrder = orderRepository.save(order);

        log.info("Order created id={} status={}", savedOrder.getId(), savedOrder.getStatus());

        return orderMapper.orderToOrderResponseDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(orderMapper::orderToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::orderToOrderResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponseDTO cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order already cancelled");
        }

        if (order.getStatus() == OrderStatus.RESERVED) {
            for (OrderItem item : order.getItems()) {
                inventoryServiceClient.releaseStock(item.getProductId(), item.getQuantity());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order updatedOrder = orderRepository.save(order);

        return orderMapper.orderToOrderResponseDTO(updatedOrder);
    }

    private void releaseReservedStockForOrder(List<OrderItemDTO> items) {
        for (OrderItemDTO itemDTO : items) {
            inventoryServiceClient.releaseStock(itemDTO.getProductId(), itemDTO.getQuantity());
        }
    }
}