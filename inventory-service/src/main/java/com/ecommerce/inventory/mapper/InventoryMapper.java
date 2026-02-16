package com.ecommerce.inventory.mapper;

import com.ecommerce.inventory.dto.InventoryRequestDTO;
import com.ecommerce.inventory.dto.InventoryResponseDTO;
import com.ecommerce.inventory.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InventoryMapper {

    InventoryResponseDTO inventoryToInventoryResponseDTO(Inventory inventory);

    Inventory inventoryRequestDTOToInventory(InventoryRequestDTO inventoryRequestDTO);
}
