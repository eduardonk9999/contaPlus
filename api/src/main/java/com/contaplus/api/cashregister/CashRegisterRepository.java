package com.contaplus.api.cashregister;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashRegisterRepository extends JpaRepository<CashRegister, UUID> {

    Optional<CashRegister> findByStore_IdAndStatus(UUID storeId, CashRegisterStatus status);

    List<CashRegister> findByStore_IdOrderByOpenedAtDesc(UUID storeId);

    boolean existsByStore_IdAndStatus(UUID storeId, CashRegisterStatus status);
}
