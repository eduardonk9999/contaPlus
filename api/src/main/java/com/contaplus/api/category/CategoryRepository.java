package com.contaplus.api.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByStore_IdAndActiveTrue(UUID storeId);

    List<Category> findByStore_Id(UUID storeId);

    Optional<Category> findByStore_IdAndNameIgnoreCase(UUID storeId, String name);

    boolean existsByStore_IdAndNameIgnoreCaseAndActiveTrue(UUID storeId, String name);
}
