package com.contaplus.api.product;

import com.contaplus.api.TestcontainersConfiguration;
import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class ProductServiceTest {

    @Autowired
    ProductService service;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    StoreRepository storeRepository;

    Store store;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        storeRepository.deleteAll();
        store = storeRepository.save(new Store("Loja Teste"));
    }

    @Test
    void deveCriarUmProduto() {
        Product product = service.criar(
            store.getId(),
            "Camiseta",
            ProductType.SELLABLE,
            3000,
            5000,
            StockUnit.UNIT
        );

        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo("Camiseta");
        assertThat(product.getType()).isEqualTo(ProductType.SELLABLE);
        assertThat(product.getCostPriceCents()).isEqualTo(3000);
        assertThat(product.getSalePriceCents()).isEqualTo(5000);
        assertThat(product.getActive()).isTrue();
    }

    @Test
    void deveRejeitarProdutoDuplicado() {
        service.criar(store.getId(), "Produto Duplicado", ProductType.SELLABLE, 100, 200, StockUnit.UNIT);

        assertThatThrownBy(() ->
            service.criar(store.getId(), "Produto Duplicado", ProductType.SELLABLE, 100, 200, StockUnit.UNIT)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("already exists");
    }

    @Test
    void deveBuscarProdutoPorId() {
        Product created = service.criar(store.getId(), "Produto Busca", ProductType.SELLABLE, 100, 200, StockUnit.UNIT);

        Product found = service.buscarPorId(created.getId());

        assertThat(found.getName()).isEqualTo("Produto Busca");
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoExiste() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> service.buscarPorId(randomId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveListarProdutosAtivosDaStore() {
        service.criar(store.getId(), "Produto 1", ProductType.SELLABLE, 100, 200, StockUnit.UNIT);
        Product inativo = service.criar(store.getId(), "Produto 2", ProductType.SUPPLY, 50, 100, StockUnit.KG);
        service.desativar(inativo.getId());

        List<Product> ativos = service.listarPorStore(store.getId(), true);
        List<Product> todos = service.listarPorStore(store.getId(), false);

        assertThat(ativos).hasSize(1);
        assertThat(todos).hasSize(2);
    }

    @Test
    void deveAtualizarProduto() {
        Product created = service.criar(store.getId(), "Produto Original", ProductType.SELLABLE, 100, 200, StockUnit.UNIT);

        Product updated = service.atualizar(
            created.getId(),
            "Produto Atualizado",
            ProductType.SUPPLY,
            150,
            300,
            StockUnit.KG,
            BigDecimal.valueOf(5)
        );

        assertThat(updated.getName()).isEqualTo("Produto Atualizado");
        assertThat(updated.getType()).isEqualTo(ProductType.SUPPLY);
        assertThat(updated.getCostPriceCents()).isEqualTo(150);
        assertThat(updated.getMinStockQuantity()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void deveDesativarProduto() {
        Product created = service.criar(store.getId(), "Produto Desativar", ProductType.SELLABLE, 100, 200, StockUnit.UNIT);

        service.desativar(created.getId());

        Product updated = service.buscarPorId(created.getId());
        assertThat(updated.getActive()).isFalse();
    }
}
