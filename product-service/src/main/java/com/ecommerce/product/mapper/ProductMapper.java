package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.ProductRequestDTO;
import com.ecommerce.product.dto.ProductResponseDTO;
import com.ecommerce.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Product entity and DTO conversions.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {

    /**
     * Convert ProductRequestDTO to Product entity
     */
    Product toEntity(ProductRequestDTO dto);

    /**
     * Convert Product entity to ProductResponseDTO
     */
    ProductResponseDTO toDTO(Product entity);

    /**
     * Update existing Product entity with data from ProductRequestDTO
     */
    void updateEntityFromDTO(ProductRequestDTO dto, @MappingTarget Product entity);
}
