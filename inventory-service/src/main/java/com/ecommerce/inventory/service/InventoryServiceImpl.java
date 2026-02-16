package com.ecommerce.inventory.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.dto.ConfirmRequestDTO;
import com.ecommerce.inventory.dto.InventoryRequestDTO;
import com.ecommerce.inventory.dto.InventoryResponseDTO;
import com.ecommerce.inventory.dto.ReleaseRequestDTO;
import com.ecommerce.inventory.dto.ReserveRequestDTO;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.mapper.InventoryMapper;
import com.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    public InventoryResponseDTO createInventory(InventoryRequestDTO inventoryRequestDTO) {
        log.info("Creating inventory for product ID: {}", inventoryRequestDTO.getProductId());

        // Check if inventory already exists for this product
        if (inventoryRepository.findByProductId(inventoryRequestDTO.getProductId()).isPresent()) {
            throw new BusinessException("Inventory already exists for product ID: " + inventoryRequestDTO.getProductId());
        }

        Inventory inventory = inventoryMapper.inventoryRequestDTOToInventory(inventoryRequestDTO);
        inventory.setReservedQuantity(0);
        
        Inventory savedInventory = inventoryRepository.save(inventory);
        log.info("Inventory created successfully for product ID: {}", savedInventory.getProductId());
        
        return inventoryMapper.inventoryToInventoryResponseDTO(savedInventory);
    }

    @Override
    public InventoryResponseDTO updateInventory(Long productId, InventoryRequestDTO inventoryRequestDTO) {
        log.info("Updating inventory for product ID: {}", productId);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product ID: " + productId));

        inventory.setAvailableQuantity(inventoryRequestDTO.getAvailableQuantity());
        inventory.setUpdatedAt(java.time.LocalDateTime.now());
        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Inventory updated successfully for product ID: {}", productId);
        
        return inventoryMapper.inventoryToInventoryResponseDTO(updatedInventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponseDTO getInventoryByProductId(Long productId) {
        log.info("Fetching inventory for product ID: {}", productId);

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product ID: " + productId));

        return inventoryMapper.inventoryToInventoryResponseDTO(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponseDTO> listAllInventory(Pageable pageable) {
        log.info("Listing inventory with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        return inventoryRepository.findAll(pageable)
                .map(inventoryMapper::inventoryToInventoryResponseDTO);
    }

    @Override
    public InventoryResponseDTO reserveStock(ReserveRequestDTO reserveRequestDTO) {
        log.info("Reserving {} units for product ID: {}", 
                reserveRequestDTO.getQuantity(), reserveRequestDTO.getProductId());

        Inventory inventory = inventoryRepository.findByProductId(reserveRequestDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product ID: " + reserveRequestDTO.getProductId()));

        // Check if enough quantity is available
        if (inventory.getAvailableQuantity() < reserveRequestDTO.getQuantity()) {
            throw new BusinessException(
                    String.format("Insufficient stock for product ID: %d. Available: %d, Requested: %d",
                            reserveRequestDTO.getProductId(),
                            inventory.getAvailableQuantity(),
                            reserveRequestDTO.getQuantity()));
        }

        // Decrease available quantity and increase reserved quantity
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - reserveRequestDTO.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() + reserveRequestDTO.getQuantity());

        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Stock reserved successfully for product ID: {}", reserveRequestDTO.getProductId());

        return inventoryMapper.inventoryToInventoryResponseDTO(updatedInventory);
    }

    @Override
    public InventoryResponseDTO releaseStock(ReleaseRequestDTO releaseRequestDTO) {
        log.info("Releasing {} units for product ID: {}", 
                releaseRequestDTO.getQuantity(), releaseRequestDTO.getProductId());

        Inventory inventory = inventoryRepository.findByProductId(releaseRequestDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product ID: " + releaseRequestDTO.getProductId()));

        // Check if enough reserved quantity exists
        if (inventory.getReservedQuantity() < releaseRequestDTO.getQuantity()) {
            throw new BusinessException(
                    String.format("Cannot release %d units for product ID: %d. Reserved: %d",
                            releaseRequestDTO.getQuantity(),
                            releaseRequestDTO.getProductId(),
                            inventory.getReservedQuantity()));
        }

        // Increase available quantity and decrease reserved quantity
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + releaseRequestDTO.getQuantity());
        inventory.setReservedQuantity(inventory.getReservedQuantity() - releaseRequestDTO.getQuantity());

        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Stock released successfully for product ID: {}", releaseRequestDTO.getProductId());

        return inventoryMapper.inventoryToInventoryResponseDTO(updatedInventory);
    }

    @Override
    public InventoryResponseDTO confirmStock(ConfirmRequestDTO confirmRequestDTO) {
        log.info("Confirming {} units for product ID: {}", 
                confirmRequestDTO.getQuantity(), confirmRequestDTO.getProductId());

        Inventory inventory = inventoryRepository.findByProductId(confirmRequestDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product ID: " + confirmRequestDTO.getProductId()));

        // Check if enough reserved quantity exists
        if (inventory.getReservedQuantity() < confirmRequestDTO.getQuantity()) {
            throw new BusinessException(
                    String.format("Cannot confirm %d units for product ID: %d. Reserved: %d",
                            confirmRequestDTO.getQuantity(),
                            confirmRequestDTO.getProductId(),
                            inventory.getReservedQuantity()));
        }

        // Just decrease reserved quantity (stock is now permanently removed)
        inventory.setReservedQuantity(inventory.getReservedQuantity() - confirmRequestDTO.getQuantity());

        Inventory updatedInventory = inventoryRepository.save(inventory);
        log.info("Stock confirmed successfully for product ID: {}", confirmRequestDTO.getProductId());

        return inventoryMapper.inventoryToInventoryResponseDTO(updatedInventory);
    }
}
