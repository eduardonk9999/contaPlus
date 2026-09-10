package com.contaplus.api.product;

import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final StoreService storeService;

    ProductService(ProductRepository repository, StoreService storeService) {
        this.repository = repository;
        this.storeService = storeService;
    }

    @Transactional
    public Product criar(UUID storeId, String name, ProductType type, Integer costPriceCents,
                         Integer salePriceCents, StockUnit stockUnit) {
        Store store = storeService.buscarPorId(storeId);

        if (repository.existsByStore_IdAndNameIgnoreCaseAndActiveTrue(storeId, name)) {
            throw new IllegalArgumentException("Product with name '" + name + "' already exists in this store");
        }

        Product product = new Product(store, name, type, costPriceCents, salePriceCents, stockUnit);
        return repository.save(product);
    }

    public Product buscarPorId(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public Product buscarPorIdEStore(UUID id, UUID storeId) {
        return repository.findByIdAndStore_Id(id, storeId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public List<Product> listarPorStore(UUID storeId, boolean apenasAtivos) {
        if (apenasAtivos) {
            return repository.findByStore_IdAndActiveTrue(storeId);
        }
        return repository.findByStore_Id(storeId);
    }

    @Transactional
    public Product atualizar(UUID id, String name, ProductType type, Integer costPriceCents,
                             Integer salePriceCents, StockUnit stockUnit, BigDecimal minStockQuantity) {
        Product product = buscarPorId(id);

        var existing = repository.findByStore_IdAndNameIgnoreCase(product.getStoreId(), name);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Product with name '" + name + "' already exists in this store");
        }

        product.update(name, type, costPriceCents, salePriceCents, stockUnit, minStockQuantity);
        return repository.save(product);
    }

    @Transactional
    public void desativar(UUID id) {
        Product product = buscarPorId(id);
        product.deactivate();
        repository.save(product);
    }
}
