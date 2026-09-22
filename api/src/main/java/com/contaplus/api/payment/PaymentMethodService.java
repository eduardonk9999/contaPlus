package com.contaplus.api.payment;

import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentMethodService {

    private final PaymentMethodRepository repository;
    private final StoreService storeService;

    PaymentMethodService(PaymentMethodRepository repository, StoreService storeService) {
        this.repository = repository;
        this.storeService = storeService;
    }

    @Transactional
    public PaymentMethod criar(UUID storeId, String name, PaymentMethodType type, boolean acceptsChange) {
        Store store = storeService.buscarPorId(storeId);

        if (repository.existsByStore_IdAndNameIgnoreCaseAndActiveTrue(storeId, name)) {
            throw new IllegalArgumentException("Payment method '" + name + "' already exists in this store");
        }

        PaymentMethod paymentMethod = new PaymentMethod(store, name, type, acceptsChange);
        return repository.save(paymentMethod);
    }

    public PaymentMethod buscarPorId(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", id));
    }

    public List<PaymentMethod> listarPorStore(UUID storeId, boolean includeInactive) {
        if (includeInactive) {
            return repository.findByStore_Id(storeId);
        }
        return repository.findByStore_IdAndActiveTrue(storeId);
    }

    @Transactional
    public PaymentMethod atualizar(UUID id, String name, PaymentMethodType type, boolean acceptsChange) {
        PaymentMethod paymentMethod = buscarPorId(id);

        var existing = repository.findByStore_IdAndNameIgnoreCase(paymentMethod.getStoreId(), name);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Payment method '" + name + "' already exists in this store");
        }

        paymentMethod.update(name, type, acceptsChange);
        return repository.save(paymentMethod);
    }

    @Transactional
    public void desativar(UUID id) {
        PaymentMethod paymentMethod = buscarPorId(id);
        paymentMethod.deactivate();
        repository.save(paymentMethod);
    }

    @Transactional
    public void criarPadroesParaLoja(Store store) {
        repository.save(new PaymentMethod(store, "Dinheiro", PaymentMethodType.CASH, true));
        repository.save(new PaymentMethod(store, "PIX", PaymentMethodType.PIX, false));
        repository.save(new PaymentMethod(store, "Cartão de Crédito", PaymentMethodType.CREDIT_CARD, false));
        repository.save(new PaymentMethod(store, "Cartão de Débito", PaymentMethodType.DEBIT_CARD, false));
    }
}
