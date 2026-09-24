package com.contaplus.api.category;

import com.contaplus.api.security.StoreAuthorizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/categories")
@Tag(name = "Categorias", description = "Gerenciamento de categorias de produtos")
public class CategoryController {

    private final CategoryService categoryService;
    private final StoreAuthorizationService storeAuth;

    CategoryController(CategoryService categoryService, StoreAuthorizationService storeAuth) {
        this.categoryService = categoryService;
        this.storeAuth = storeAuth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@storeAuthorizationService.isOwner()")
    public CategoryResponse criar(@Valid @RequestBody CriarCategoryRequest request) {
        storeAuth.validateStoreAccess(request.storeId());
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
        storeAuth.validateStoreAccess(category.getStoreId());
        return toResponse(category);
    }

    @GetMapping
    public List<CategoryResponse> listar(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        UUID authorizedStoreId = storeId != null ? storeId : storeAuth.getCurrentUserStoreId();
        storeAuth.validateStoreAccess(authorizedStoreId);
        return categoryService.listarPorStore(authorizedStoreId, includeInactive).stream()
            .map(this::toResponse)
            .toList();
    }

    @PutMapping("/{id}")
    @PreAuthorize("@storeAuthorizationService.isOwner()")
    public CategoryResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarCategoryRequest request) {
        Category existing = categoryService.buscarPorId(id);
        storeAuth.validateStoreAccess(existing.getStoreId());

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
    @PreAuthorize("@storeAuthorizationService.isOwner()")
    public void desativar(@PathVariable UUID id) {
        Category existing = categoryService.buscarPorId(id);
        storeAuth.validateStoreAccess(existing.getStoreId());
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
