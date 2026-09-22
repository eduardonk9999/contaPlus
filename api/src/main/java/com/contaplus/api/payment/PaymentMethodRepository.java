package com.contaplus.api.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    List<PaymentMethod> findByStore_IdAndActiveTrue(UUID storeId);

    List<PaymentMethod> findByStore_Id(UUID storeId);

    Optional<PaymentMethod> findByStore_IdAndNameIgnoreCase(UUID storeId, String name);

    boolean existsByStore_IdAndNameIgnoreCaseAndActiveTrue(UUID storeId, String name);
}
