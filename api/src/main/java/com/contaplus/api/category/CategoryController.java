package com.contaplus.api.category;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse criar(@Valid @RequestBody CriarCategoryRequest request) {
        CategoryService.CriarCategoryRequest serviceRequest = new CategoryService.CriarCategoryRequest(
            request.storeId(),
            request.name(),
            request.description(),
            request.color()
        );

        Category category = categoryService.criar(serviceRequest);
        return toResponse(category);
    }

    @GetMapping("/{id}")
    public CategoryResponse buscarPorId(@PathVariable UUID id) {
        Category category = categoryService.buscarPorId(id);
        return toResponse(category);
    }

    @GetMapping
    public List<CategoryResponse> listar(
            @RequestParam UUID storeId,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return categoryService.listarPorStore(storeId, includeInactive).stream()
            .map(this::toResponse)
            .toList();
    }

    @PutMapping("/{id}")
    public CategoryResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarCategoryRequest request) {
        CategoryService.AtualizarCategoryRequest serviceRequest = new CategoryService.AtualizarCategoryRequest(
            request.name(),
            request.description(),
            request.color()
        );

        Category category = categoryService.atualizar(id, serviceRequest);
        return toResponse(category);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable UUID id) {
        categoryService.desativar(id);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getStoreId(),
            category.getName(),
            category.getDescription(),
            category.getColor(),
            category.getActive(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }

    record CriarCategoryRequest(
        @NotNull(message = "storeId is required") UUID storeId,
        @NotBlank(message = "name is required") String name,
        String description,
        String color
    ) {}

    record AtualizarCategoryRequest(
        @NotBlank(message = "name is required") String name,
        String description,
        String color
    ) {}

    record CategoryResponse(
        UUID id,
        UUID storeId,
        String name,
        String description,
        String color,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
    ) {}
}
