package com.contaplus.api.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Optional<Expense> findByStore_IdAndIdempotencyKey(UUID storeId, UUID idempotencyKey);

    Page<Expense> findByStore_IdAndOccurredAtBetween(UUID storeId, OffsetDateTime start, OffsetDateTime end,
                                                     Pageable pageable);
}
