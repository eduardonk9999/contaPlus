package com.contaplus.api.stock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByProductIdOrderByCreatedAtDesc(UUID productId);

    List<StockMovement> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    List<StockMovement> findByTransactionId(UUID transactionId);
}
