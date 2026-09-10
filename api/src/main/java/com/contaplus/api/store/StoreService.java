package com.contaplus.api.store;

import com.contaplus.api.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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

    public Store buscarPorId(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Store", id));
    }

    public List<Store> listarTodas() {
        return repository.findAll();
    }

    @Transactional
    public Store atualizar(UUID id, String nome) {
        Store store = buscarPorId(id);
        store.updateName(nome);
        return repository.save(store);
    }

    @Transactional
    public void deletar(UUID id) {
        Store store = buscarPorId(id);
        repository.delete(store);
    }
}
