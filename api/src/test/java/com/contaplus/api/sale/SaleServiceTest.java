package com.contaplus.api.sale;

import com.contaplus.api.TestcontainersConfiguration;
import com.contaplus.api.product.Product;
import com.contaplus.api.product.ProductRepository;
import com.contaplus.api.product.ProductType;
import com.contaplus.api.product.StockUnit;
import com.contaplus.api.stock.StockMovementRepository;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreRepository;
import com.contaplus.api.transaction.Transaction;
import com.contaplus.api.transaction.TransactionRepository;
import com.contaplus.api.transaction.TransactionSource;
import com.contaplus.api.transaction.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class SaleServiceTest {

    @Autowired
    SaleService saleService;

    @Autowired
    SalePreviewService previewService;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    StockMovementRepository stockMovementRepository;

    Store store;
    Product product1;
    Product product2;

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        transactionRepository.deleteAll();
        productRepository.deleteAll();
        storeRepository.deleteAll();

        store = storeRepository.save(new Store("Loja Vendas"));

        product1 = new Product(store, "Camiseta", ProductType.SELLABLE, 3000, 5000, StockUnit.UNIT);
        product1.adjustStock(BigDecimal.valueOf(10));
        product1 = productRepository.save(product1);

        product2 = new Product(store, "Calcao", ProductType.SELLABLE, 2000, 4000, StockUnit.UNIT);
        product2.adjustStock(BigDecimal.valueOf(5));
        product2 = productRepository.save(product2);
    }

    @Test
    void deveConfirmarVenda() {
        UUID idempotencyKey = UUID.randomUUID();

        var request = new SaleService.ConfirmarVendaRequest(
            store.getId(),
            idempotencyKey,
            "Venda de camiseta",
            List.of(new SaleService.ItemVendaRequest(product1.getId(), BigDecimal.valueOf(2))),
            TransactionSource.MANUAL,
            null,
            OffsetDateTime.now()
        );

        Transaction transaction = saleService.confirmarVenda(request);

        assertThat(transaction.getId()).isNotNull();
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.CONFIRMED);
        assertThat(transaction.getTotalAmountCents()).isEqualTo(10000);
        assertThat(transaction.getTotalCostCents()).isEqualTo(6000);
        assertThat(transaction.getGrossProfitCents()).isEqualTo(4000);
    }

    @Test
    void deveSerIdempotente() {
        UUID idempotencyKey = UUID.randomUUID();

        var request = new SaleService.ConfirmarVendaRequest(
            store.getId(),
            idempotencyKey,
            "Venda idempotente",
            List.of(new SaleService.ItemVendaRequest(product1.getId(), BigDecimal.ONE)),
            TransactionSource.MANUAL,
            null,
            OffsetDateTime.now()
        );

        Transaction first = saleService.confirmarVenda(request);
        Transaction second = saleService.confirmarVenda(request);

        assertThat(first.getId()).isEqualTo(second.getId());

        Product updated = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualByComparingTo(BigDecimal.valueOf(9));
    }

    @Test
    void deveAtualizarEstoqueAposVenda() {
        UUID idempotencyKey = UUID.randomUUID();

        var request = new SaleService.ConfirmarVendaRequest(
            store.getId(),
            idempotencyKey,
            "Venda com estoque",
            List.of(new SaleService.ItemVendaRequest(product1.getId(), BigDecimal.valueOf(3))),
            TransactionSource.MANUAL,
            null,
            OffsetDateTime.now()
        );

        saleService.confirmarVenda(request);

        Product updated = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualByComparingTo(BigDecimal.valueOf(7));
    }

    @Test
    void deveCalcularMargemCorretamente() {
        UUID idempotencyKey = UUID.randomUUID();

        var request = new SaleService.ConfirmarVendaRequest(
            store.getId(),
            idempotencyKey,
            "Venda com margem",
            List.of(
                new SaleService.ItemVendaRequest(product1.getId(), BigDecimal.valueOf(2)),
                new SaleService.ItemVendaRequest(product2.getId(), BigDecimal.ONE)
            ),
            TransactionSource.MANUAL,
            null,
            OffsetDateTime.now()
        );

        Transaction transaction = saleService.confirmarVenda(request);

        assertThat(transaction.getTotalAmountCents()).isEqualTo(14000);
        assertThat(transaction.getTotalCostCents()).isEqualTo(8000);
        assertThat(transaction.getGrossProfitCents()).isEqualTo(6000);
        assertThat(transaction.getMarginPercent().doubleValue()).isCloseTo(42.86, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void deveCongelarCustoNoMomentoDaVenda() {
        product1.update(product1.getName(), product1.getType(), 5000, product1.getSalePriceCents(),
            product1.getStockUnit(), product1.getMinStockQuantity());
        productRepository.save(product1);

        UUID idempotencyKey = UUID.randomUUID();

        var request = new SaleService.ConfirmarVendaRequest(
            store.getId(),
            idempotencyKey,
            "Venda com custo congelado",
            List.of(new SaleService.ItemVendaRequest(product1.getId(), BigDecimal.ONE)),
            TransactionSource.MANUAL,
            null,
            OffsetDateTime.now()
        );

        Transaction transaction = saleService.confirmarVenda(request);

        assertThat(transaction.getItems().get(0).getUnitCostCents()).isEqualTo(5000);
    }

    @Test
    void deveRetornarPreviewSemGravar() {
        var request = new SalePreviewService.PreviewVendaRequest(
            store.getId(),
            List.of(new SalePreviewService.PreviewItemRequest(product1.getId(), BigDecimal.valueOf(2)))
        );

        SalePreviewService.SalePreview preview = previewService.calcularPreview(request);

        assertThat(preview.totalAmountCents()).isEqualTo(10000);
        assertThat(preview.totalCostCents()).isEqualTo(6000);

        Product unchanged = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(unchanged.getStockQuantity()).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void previewDeveRetornarWarningsDeEstoqueBaixo() {
        product1.adjustStock(BigDecimal.valueOf(-9));
        productRepository.save(product1);

        var request = new SalePreviewService.PreviewVendaRequest(
            store.getId(),
            List.of(new SalePreviewService.PreviewItemRequest(product1.getId(), BigDecimal.valueOf(2)))
        );

        SalePreviewService.SalePreview preview = previewService.calcularPreview(request);

        assertThat(preview.warnings()).anyMatch(w -> w.contains("negative stock"));
    }

    @Test
    void deveListarVendasDaStore() {
        UUID key1 = UUID.randomUUID();
        UUID key2 = UUID.randomUUID();

        saleService.confirmarVenda(new SaleService.ConfirmarVendaRequest(
            store.getId(), key1, "Venda 1",
            List.of(new SaleService.ItemVendaRequest(product1.getId(), BigDecimal.ONE)),
            TransactionSource.MANUAL, null, OffsetDateTime.now()
        ));

        saleService.confirmarVenda(new SaleService.ConfirmarVendaRequest(
            store.getId(), key2, "Venda 2",
            List.of(new SaleService.ItemVendaRequest(product2.getId(), BigDecimal.ONE)),
            TransactionSource.MANUAL, null, OffsetDateTime.now()
        ));

        List<Transaction> vendas = saleService.listarVendasPorStore(store.getId());

        assertThat(vendas).hasSize(2);
    }

    @Test
    void deveRejeitarProdutoInativo() {
        product1.deactivate();
        productRepository.save(product1);

        var request = new SaleService.ConfirmarVendaRequest(
            store.getId(),
            UUID.randomUUID(),
            "Venda produto inativo",
            List.of(new SaleService.ItemVendaRequest(product1.getId(), BigDecimal.ONE)),
            TransactionSource.MANUAL,
            null,
            OffsetDateTime.now()
        );

        assertThatThrownBy(() -> saleService.confirmarVenda(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not active");
    }

    @Test
    void deveRejeitarProdutoDeOutraStore() {
        Store outraStore = storeRepository.save(new Store("Outra Loja"));
        Product outroProduto = productRepository.save(
            new Product(outraStore, "Outro Produto", ProductType.SELLABLE, 100, 200, StockUnit.UNIT)
        );

        var request = new SaleService.ConfirmarVendaRequest(
            store.getId(),
            UUID.randomUUID(),
            "Venda produto outra store",
            List.of(new SaleService.ItemVendaRequest(outroProduto.getId(), BigDecimal.ONE)),
            TransactionSource.MANUAL,
            null,
            OffsetDateTime.now()
        );

        assertThatThrownBy(() -> saleService.confirmarVenda(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong");
    }
}
