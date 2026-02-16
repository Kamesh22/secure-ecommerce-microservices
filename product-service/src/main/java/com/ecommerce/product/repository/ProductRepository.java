package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Search products by name (case-insensitive)
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Search products by name with pagination
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
