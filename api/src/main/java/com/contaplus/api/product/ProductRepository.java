package com.contaplus.api.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    List<Product> findByStore_IdAndActiveTrue(UUID storeId);

    List<Product> findByStore_Id(UUID storeId);

    Page<Product> findByStore_IdAndActiveTrue(UUID storeId, Pageable pageable);

    Page<Product> findByStore_Id(UUID storeId, Pageable pageable);

    Page<Product> findByStore_IdAndNameContainingIgnoreCaseAndActiveTrue(UUID storeId, String name, Pageable pageable);

    Optional<Product> findByIdAndStore_Id(UUID id, UUID storeId);

    Optional<Product> findByStore_IdAndNameIgnoreCase(UUID storeId, String name);

    boolean existsByStore_IdAndNameIgnoreCaseAndActiveTrue(UUID storeId, String name);
}
