package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequestDTO;
import com.ecommerce.product.dto.ProductResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    /**
     * Create a new product
     */
    ProductResponseDTO createProduct(ProductRequestDTO requestDTO);

    /**
     * Get all products with pagination
     */
    Page<ProductResponseDTO> getAllProducts(Pageable pageable);

    /**
     * Get product by ID
     */
    ProductResponseDTO getProductById(Long id);

    /**
     * Update existing product
     */
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO requestDTO);

    /**
     * Delete product by ID
     */
    void deleteProduct(Long id);

    /**
     * Search products by name
     */
    List<ProductResponseDTO> searchProductsByName(String name);
}
