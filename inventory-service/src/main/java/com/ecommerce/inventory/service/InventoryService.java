package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.ConfirmRequestDTO;
import com.ecommerce.inventory.dto.InventoryRequestDTO;
import com.ecommerce.inventory.dto.InventoryResponseDTO;
import com.ecommerce.inventory.dto.ReleaseRequestDTO;
import com.ecommerce.inventory.dto.ReserveRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {

    /**
     * Create a new inventory record
     */
    InventoryResponseDTO createInventory(InventoryRequestDTO inventoryRequestDTO);

    /**
     * Update an existing inventory record
     */
    InventoryResponseDTO updateInventory(Long productId, InventoryRequestDTO inventoryRequestDTO);

    /**
     * Get inventory by product ID
     */
    InventoryResponseDTO getInventoryByProductId(Long productId);

    /**
     * List all inventory records with pagination
     */
    Page<InventoryResponseDTO> listAllInventory(Pageable pageable);

    /**
     * Reserve stock for an order
     * Decreases availableQuantity and increases reservedQuantity
     */
    InventoryResponseDTO reserveStock(ReserveRequestDTO reserveRequestDTO);

    /**
     * Release reserved stock (when order is canceled)
     * Decreases reservedQuantity and increases availableQuantity
     */
    InventoryResponseDTO releaseStock(ReleaseRequestDTO releaseRequestDTO);

    /**
     * Confirm reservation (when order is confirmed/paid)
     * Decreases reservedQuantity (stock is now permanently removed from inventory)
     */
    InventoryResponseDTO confirmStock(ConfirmRequestDTO confirmRequestDTO);
}
