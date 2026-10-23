package com.contaplus.api.expense;

import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreService;
import com.contaplus.api.transaction.TransactionSource;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;
    private final StoreService storeService;

    ExpenseService(ExpenseRepository repository, StoreService storeService) {
        this.repository = repository;
        this.storeService = storeService;
    }

    /** Repetir a mesma idempotencyKey devolve a despesa já registrada, sem duplicar. */
    @Transactional
    public Expense registrar(RegistrarDespesa request) {
        var existente = repository.findByStore_IdAndIdempotencyKey(request.storeId(), request.idempotencyKey());
        if (existente.isPresent()) {
            return existente.get();
        }

        if (request.amountCents() == null || request.amountCents() <= 0) {
            throw new IllegalArgumentException("O valor da despesa precisa ser maior que zero");
        }
        if (request.description() == null || request.description().isBlank()) {
            throw new IllegalArgumentException("Informe a descrição da despesa");
        }

        Store store = storeService.buscarPorId(request.storeId());
        Expense expense = new Expense(
            store,
            request.description().trim(),
            request.amountCents(),
            request.category() != null ? request.category() : ExpenseCategory.OTHER,
            request.occurredAt() != null ? request.occurredAt() : OffsetDateTime.now(),
            request.source() != null ? request.source() : TransactionSource.MANUAL,
            request.originalInput(),
            request.idempotencyKey()
        );
        return repository.save(expense);
    }

    public Page<Expense> listar(UUID storeId, OffsetDateTime inicio, OffsetDateTime fim, Pageable pageable) {
        return repository.findByStore_IdAndOccurredAtBetween(storeId, inicio, fim, pageable);
    }

    public record RegistrarDespesa(
        UUID storeId,
        UUID idempotencyKey,
        String description,
        Integer amountCents,
        ExpenseCategory category,
        OffsetDateTime occurredAt,
        TransactionSource source,
        String originalInput
    ) {}
}
