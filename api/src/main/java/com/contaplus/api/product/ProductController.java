package com.contaplus.api.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/products")
public class ProductController {

    private final ProductService service;

    ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse criar(@Valid @RequestBody CriarProductRequest request) {
        Product product = service.criar(
            request.storeId(),
            request.name(),
            request.type() != null ? request.type() : ProductType.SELLABLE,
            request.costPriceCents(),
            request.salePriceCents(),
            request.stockUnit() != null ? request.stockUnit() : StockUnit.UNIT
        );
        return toResponse(product);
    }

    @GetMapping("/{id}")
    public ProductResponse buscarPorId(@PathVariable UUID id) {
        Product product = service.buscarPorId(id);
        return toResponse(product);
    }

    @GetMapping
    public List<ProductResponse> listarPorStore(
            @RequestParam UUID storeId,
            @RequestParam(defaultValue = "true") boolean apenasAtivos) {
        return service.listarPorStore(storeId, apenasAtivos).stream()
            .map(this::toResponse)
            .toList();
    }

    @PutMapping("/{id}")
    public ProductResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarProductRequest request) {
        Product product = service.atualizar(
            id,
            request.name(),
            request.type(),
            request.costPriceCents(),
            request.salePriceCents(),
            request.stockUnit(),
            request.minStockQuantity() != null ? request.minStockQuantity() : BigDecimal.ZERO
        );
        return toResponse(product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable UUID id) {
        service.desativar(id);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getStoreId(),
            product.getName(),
            product.getType(),
            product.getCostPriceCents(),
            product.getSalePriceCents(),
            product.getStockQuantity(),
            product.getStockUnit(),
            product.getMinStockQuantity(),
            product.getActive()
        );
    }

    record CriarProductRequest(
        @NotNull(message = "storeId is required") UUID storeId,
        @NotBlank(message = "name is required") String name,
        ProductType type,
        @NotNull(message = "costPriceCents is required") @Min(0) Integer costPriceCents,
        @NotNull(message = "salePriceCents is required") @Min(0) Integer salePriceCents,
        StockUnit stockUnit
    ) {}

    record AtualizarProductRequest(
        @NotBlank(message = "name is required") String name,
        @NotNull(message = "type is required") ProductType type,
        @NotNull(message = "costPriceCents is required") @Min(0) Integer costPriceCents,
        @NotNull(message = "salePriceCents is required") @Min(0) Integer salePriceCents,
        @NotNull(message = "stockUnit is required") StockUnit stockUnit,
        BigDecimal minStockQuantity
    ) {}

    record ProductResponse(
        UUID id,
        UUID storeId,
        String name,
        ProductType type,
        Integer costPriceCents,
        Integer salePriceCents,
        BigDecimal stockQuantity,
        StockUnit stockUnit,
        BigDecimal minStockQuantity,
        Boolean active
    ) {}
}
