package com.contaplus.api.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByStore_IdAndActiveTrue(UUID storeId);

    List<Product> findByStore_Id(UUID storeId);

    Optional<Product> findByIdAndStore_Id(UUID id, UUID storeId);

    Optional<Product> findByStore_IdAndNameIgnoreCase(UUID storeId, String name);

    boolean existsByStore_IdAndNameIgnoreCaseAndActiveTrue(UUID storeId, String name);
}
