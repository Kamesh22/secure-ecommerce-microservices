package com.ecommerce.inventory.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.inventory.dto.ConfirmRequestDTO;
import com.ecommerce.inventory.dto.InventoryRequestDTO;
import com.ecommerce.inventory.dto.InventoryResponseDTO;
import com.ecommerce.inventory.dto.ReleaseRequestDTO;
import com.ecommerce.inventory.dto.ReserveRequestDTO;
import com.ecommerce.inventory.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * PUBLIC API
     */
    @GetMapping("/{productId}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get inventory by product ID", 
            description = "Retrieve inventory details for a specific product. Public access allowed.")
    @Tag(name = "Public")
    public ResponseEntity<InventoryResponseDTO> getInventoryByProductId(@PathVariable Long productId) {
        log.info("GET /api/inventory/{} - Fetching inventory", productId);
        InventoryResponseDTO response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(response);
    }

    /**
     * ADMIN APIs
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new inventory", 
            description = "Create a new inventory record for a product. Admin only.")
    @Tag(name = "Admin")
    public ResponseEntity<InventoryResponseDTO> createInventory(
            @Valid @RequestBody InventoryRequestDTO inventoryRequestDTO) {
        log.info("POST /api/inventory - Creating new inventory for product ID: {}", 
                inventoryRequestDTO.getProductId());
        InventoryResponseDTO response = inventoryService.createInventory(inventoryRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update inventory", 
            description = "Update the available quantity for a product. Admin only.")
    @Tag(name = "Admin")
    public ResponseEntity<InventoryResponseDTO> updateInventory(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryRequestDTO inventoryRequestDTO) {
        log.info("PUT /api/inventory/{} - Updating inventory", productId);
        InventoryResponseDTO response = inventoryService.updateInventory(productId, inventoryRequestDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "List all inventory",
        description = "Retrieve paginated inventory records. Admin only."
    )
    @Tag(name = "Admin")
    public ResponseEntity<Page<InventoryResponseDTO>> listAllInventory(
            @ParameterObject Pageable pageable) {

        log.info("GET /api/inventory - Listing inventory with pagination");

        return ResponseEntity.ok(inventoryService.listAllInventory(pageable));
    }

    /**
     * INTERNAL API: Reserve stock for an order
     */
    @PostMapping("/reserve")
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(summary = "Reserve stock", 
            description = "Reserve stock for an order. Internal use only (ORDER_SERVICE).")
    @Tag(name = "Internal")
    public ResponseEntity<InventoryResponseDTO> reserveStock(
            @Valid @RequestBody ReserveRequestDTO reserveRequestDTO) {
        log.info("POST /api/inventory/reserve - Reserving {} units for product ID: {}", 
                reserveRequestDTO.getQuantity(), reserveRequestDTO.getProductId());
        InventoryResponseDTO response = inventoryService.reserveStock(reserveRequestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/release")
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(summary = "Release reserved stock", 
            description = "Release previously reserved stock (e.g., order cancellation). Internal use only (ORDER_SERVICE).")
    @Tag(name = "Internal")
    public ResponseEntity<InventoryResponseDTO> releaseStock(
            @Valid @RequestBody ReleaseRequestDTO releaseRequestDTO) {
        log.info("POST /api/inventory/release - Releasing {} units for product ID: {}", 
                releaseRequestDTO.getQuantity(), releaseRequestDTO.getProductId());
        InventoryResponseDTO response = inventoryService.releaseStock(releaseRequestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('INTERNAL')")
    @Operation(summary = "Confirm reserved stock", 
            description = "Confirm reserved stock (convert to permanent deduction). Internal use only (ORDER_SERVICE).")
    @Tag(name = "Internal")
    public ResponseEntity<InventoryResponseDTO> confirmStock(
            @Valid @RequestBody ConfirmRequestDTO confirmRequestDTO) {
        log.info("POST /api/inventory/confirm - Confirming {} units for product ID: {}", 
                confirmRequestDTO.getQuantity(), confirmRequestDTO.getProductId());
        InventoryResponseDTO response = inventoryService.confirmStock(confirmRequestDTO);
        return ResponseEntity.ok(response);
    }
}
