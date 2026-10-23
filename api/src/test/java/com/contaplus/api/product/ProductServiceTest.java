package com.contaplus.api.product;

import com.contaplus.api.DatabaseCleaner;
import com.contaplus.api.TestcontainersConfiguration;
import com.contaplus.api.exception.ResourceNotFoundException;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

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
    JdbcTemplate jdbc;

    @Autowired
    StoreRepository storeRepository;

    Store store;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.limpar(jdbc);
        store =storeRepository.save(new Store("Loja Teste"));
    }

    @Test
    void deveCriarUmProduto() {
        Product product = service.criar(
            store.getId(),
            "Camiseta",
            ProductType.SELLABLE,
            3000,
            5000,
            StockUnit.UNIT,
            null,
            null
        );

        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo("Camiseta");
        assertThat(product.getType()).isEqualTo(ProductType.SELLABLE);
        assertThat(product.getCostPriceCents()).isEqualTo(3000);
        assertThat(product.getSalePriceCents()).isEqualTo(5000);
        assertThat(product.getActive()).isTrue();
    }

    @Test
    void deveCriarProdutoComEstoqueMinimo() {
        Product product = service.criar(store.getId(), "Refrigerante", ProductType.SELLABLE, 300, 600,
            StockUnit.UNIT, null, BigDecimal.valueOf(12));

        assertThat(product.getMinStockQuantity()).isEqualByComparingTo(BigDecimal.valueOf(12));
    }

    @Test
    void deveOrdenarEBuscarIgnorandoAcentos() {
        service.criar(store.getId(), "Banana", ProductType.SELLABLE, 100, 200, StockUnit.UNIT, null, null);
        service.criar(store.getId(), "Água mineral", ProductType.SELLABLE, 100, 200, StockUnit.UNIT, null, null);
        service.criar(store.getId(), "Abacaxi", ProductType.SELLABLE, 100, 200, StockUnit.UNIT, null, null);
        var porNome = PageRequest.of(0, 10, Sort.by("name"));

        var todos = service.listarComFiltros(
            new ProductFilter(store.getId(), null, null, null, true, null, null, null), porNome);
        var busca = service.listarComFiltros(
            new ProductFilter(store.getId(), "agua", null, null, true, null, null, null), porNome);

        assertThat(todos.getContent()).extracting(Product::getName)
            .containsExactly("Abacaxi", "Água mineral", "Banana");
        assertThat(busca.getContent()).extracting(Product::getName)
            .containsExactly("Água mineral");
    }

    @Test
    void devePermitirRecadastrarNomeDeProdutoDesativado() {
        Product antigo = service.criar(store.getId(), "Água", ProductType.SELLABLE, 100, 200, StockUnit.UNIT, null, null);
        service.desativar(antigo.getId());

        Product novo = service.criar(store.getId(), "Água", ProductType.SELLABLE, 150, 300, StockUnit.UNIT, null, null);
        Product editado = service.atualizar(novo.getId(), "Água", ProductType.SELLABLE, 150, 350,
            StockUnit.UNIT, BigDecimal.ZERO, null);

        assertThat(editado.getSalePriceCents()).isEqualTo(350);
    }

    @Test
    void deveRejeitarProdutoDuplicado() {
        service.criar(store.getId(), "Produto Duplicado", ProductType.SELLABLE, 100, 200, StockUnit.UNIT, null, null);

        assertThatThrownBy(() ->
            service.criar(store.getId(), "Produto Duplicado", ProductType.SELLABLE, 100, 200, StockUnit.UNIT, null, null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Já existe");
    }

    @Test
    void deveBuscarProdutoPorId() {
        Product created = service.criar(store.getId(), "Produto Busca", ProductType.SELLABLE, 100, 200, StockUnit.UNIT, null, null);

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
        service.criar(store.getId(), "Produto 1", ProductType.SELLABLE, 100, 200, StockUnit.UNIT, null, null);
        Product inativo = service.criar(store.getId(), "Produto 2", ProductType.SUPPLY, 50, 100, StockUnit.KG, null, null);
        service.desativar(inativo.getId());

        List<Product> ativos = service.listarPorStore(store.getId(), true);
        List<Product> todos = service.listarPorStore(store.getId(), false);

        assertThat(ativos).hasSize(1);
        assertThat(todos).hasSize(2);
    }

    @Test
    void deveAtualizarProduto() {
        Product created = service.criar(store.getId(), "Produto Original", ProductType.SELLABLE, 100, 200, StockUnit.UNIT, null, null);

        Product updated = service.atualizar(
            created.getId(),
            "Produto Atualizado",
            ProductType.SUPPLY,
            150,
            300,
            StockUnit.KG,
            BigDecimal.valueOf(5),
            null
        );

        assertThat(updated.getName()).isEqualTo("Produto Atualizado");
        assertThat(updated.getType()).isEqualTo(ProductType.SUPPLY);
        assertThat(updated.getCostPriceCents()).isEqualTo(150);
        assertThat(updated.getMinStockQuantity()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void deveDesativarProduto() {
        Product created = service.criar(store.getId(), "Produto Desativar", ProductType.SELLABLE, 100, 200, StockUnit.UNIT, null, null);

        service.desativar(created.getId());

        Product updated = service.buscarPorId(created.getId());
        assertThat(updated.getActive()).isFalse();
    }
}
