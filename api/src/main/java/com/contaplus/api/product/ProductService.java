package com.contaplus.api.product;

import com.contaplus.api.category.Category;
import com.contaplus.api.category.CategoryRepository;
import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;
    private final StoreService storeService;

    ProductService(ProductRepository repository, CategoryRepository categoryRepository, StoreService storeService) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.storeService = storeService;
    }

    @Transactional
    public Product criar(UUID storeId, String name, ProductType type, Integer costPriceCents,
                         Integer salePriceCents, StockUnit stockUnit, UUID categoryId) {
        Store store = storeService.buscarPorId(storeId);

        if (repository.existsByStore_IdAndNameIgnoreCaseAndActiveTrue(storeId, name)) {
            throw new IllegalArgumentException("Product with name '" + name + "' already exists in this store");
        }

        Product product = new Product(store, name, type, costPriceCents, salePriceCents, stockUnit);

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
            if (!category.getStoreId().equals(storeId)) {
                throw new IllegalArgumentException("Category does not belong to this store");
            }
            product.setCategory(category);
        }

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

    public Page<Product> listarPorStorePaginado(UUID storeId, boolean apenasAtivos, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repository.findByStore_IdAndNameContainingIgnoreCaseAndActiveTrue(storeId, search, pageable);
        }
        if (apenasAtivos) {
            return repository.findByStore_IdAndActiveTrue(storeId, pageable);
        }
        return repository.findByStore_Id(storeId, pageable);
    }

    @Transactional
    public Product atualizar(UUID id, String name, ProductType type, Integer costPriceCents,
                             Integer salePriceCents, StockUnit stockUnit, BigDecimal minStockQuantity, UUID categoryId) {
        Product product = buscarPorId(id);

        var existing = repository.findByStore_IdAndNameIgnoreCase(product.getStoreId(), name);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Product with name '" + name + "' already exists in this store");
        }

        product.update(name, type, costPriceCents, salePriceCents, stockUnit, minStockQuantity);

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
            if (!category.getStoreId().equals(product.getStoreId())) {
                throw new IllegalArgumentException("Category does not belong to this store");
            }
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        return repository.save(product);
    }

    @Transactional
    public void desativar(UUID id) {
        Product product = buscarPorId(id);
        product.deactivate();
        repository.save(product);
    }
}
