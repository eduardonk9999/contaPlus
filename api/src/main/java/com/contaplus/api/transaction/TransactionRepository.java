package com.contaplus.api.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByStore_IdAndIdempotencyKey(UUID storeId, UUID idempotencyKey);

    List<Transaction> findByStore_IdAndOccurredAtBetweenOrderByOccurredAtDesc(
        UUID storeId, OffsetDateTime start, OffsetDateTime end);

    List<Transaction> findByStore_IdOrderByOccurredAtDesc(UUID storeId);

    List<Transaction> findByStore_IdAndTypeOrderByOccurredAtDesc(UUID storeId, TransactionType type);

    Page<Transaction> findByStore_IdAndType(UUID storeId, TransactionType type, Pageable pageable);

    List<Transaction> findByCustomer_IdOrderByOccurredAtDesc(UUID customerId);

    List<Transaction> findByCustomer_IdAndTypeOrderByOccurredAtDesc(UUID customerId, TransactionType type);

    Page<Transaction> findByCustomer_IdAndType(UUID customerId, TransactionType type, Pageable pageable);
}
