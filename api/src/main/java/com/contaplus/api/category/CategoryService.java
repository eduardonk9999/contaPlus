package com.contaplus.api.category;

import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final StoreService storeService;

    CategoryService(CategoryRepository categoryRepository, StoreService storeService) {
        this.categoryRepository = categoryRepository;
        this.storeService = storeService;
    }

    @Transactional
    public Category criar(CriarCategoryRequest request) {
        Store store = storeService.buscarPorId(request.storeId());

        if (categoryRepository.existsByStore_IdAndNameIgnoreCaseAndActiveTrue(request.storeId(), request.name())) {
            throw new IllegalArgumentException("Category '" + request.name() + "' already exists in this store");
        }

        Category category = new Category(
            store,
            request.name(),
            request.description(),
            request.color()
        );

        return categoryRepository.save(category);
    }

    public Category buscarPorId(UUID id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    public List<Category> listarPorStore(UUID storeId, boolean includeInactive) {
        if (includeInactive) {
            return categoryRepository.findByStore_Id(storeId);
        }
        return categoryRepository.findByStore_IdAndActiveTrue(storeId);
    }

    @Transactional
    public Category atualizar(UUID id, AtualizarCategoryRequest request) {
        Category category = buscarPorId(id);

        var existing = categoryRepository.findByStore_IdAndNameIgnoreCase(category.getStoreId(), request.name());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Category '" + request.name() + "' already exists in this store");
        }

        category.update(request.name(), request.description(), request.color());
        return categoryRepository.save(category);
    }

    @Transactional
    public void desativar(UUID id) {
        Category category = buscarPorId(id);
        category.deactivate();
        categoryRepository.save(category);
    }

    public record CriarCategoryRequest(
        UUID storeId,
        String name,
        String description,
        String color
    ) {}

    public record AtualizarCategoryRequest(
        String name,
        String description,
        String color
    ) {}
}
