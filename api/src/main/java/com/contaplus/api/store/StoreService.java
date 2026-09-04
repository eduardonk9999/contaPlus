package com.contaplus.api.store;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class StoreService {

    private final StoreRepository repository;

    StoreService(StoreRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Store criar(String nome) {
        Store nova = new Store(nome);
        return repository.save(nova);
    }
}
