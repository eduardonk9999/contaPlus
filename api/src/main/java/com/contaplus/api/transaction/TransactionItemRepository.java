package com.contaplus.api.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionItemRepository extends JpaRepository<TransactionItem, UUID> {

    List<TransactionItem> findByTransactionId(UUID transactionId);

    List<TransactionItem> findByProductId(UUID productId);
}
