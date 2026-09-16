package com.contaplus.api.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    List<Supplier> findByStore_IdAndActiveTrue(UUID storeId);

    Page<Supplier> findByStore_IdAndActiveTrue(UUID storeId, Pageable pageable);

    Page<Supplier> findByStore_Id(UUID storeId, Pageable pageable);

    Page<Supplier> findByStore_IdAndNameContainingIgnoreCaseAndActiveTrue(UUID storeId, String name, Pageable pageable);

    Optional<Supplier> findByStore_IdAndCnpj(UUID storeId, String cnpj);
}
