package com.contaplus.api.sale;

import com.contaplus.api.DatabaseCleaner;
import com.contaplus.api.TestcontainersConfiguration;
import com.contaplus.api.payment.Payment;
import com.contaplus.api.payment.PaymentMethodRepository;
import com.contaplus.api.payment.PaymentMethodService;
import com.contaplus.api.payment.PaymentRepository;
import com.contaplus.api.payment.PaymentService;
import com.contaplus.api.product.Product;
import com.contaplus.api.product.ProductRepository;
import com.contaplus.api.product.ProductType;
import com.contaplus.api.product.StockUnit;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreRepository;
import com.contaplus.api.transaction.Transaction;
import com.contaplus.api.transaction.TransactionSource;
import com.contaplus.api.transaction.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

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
    JdbcTemplate jdbc;

    @Autowired
    PaymentMethodService paymentMethodService;

    @Autowired
    PaymentMethodRepository paymentMethodRepository;

    @Autowired
    PaymentRepository paymentRepository;

    Store store;
    Product product1;
    Product product2;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.limpar(jdbc);

        store = storeRepository.save(new Store("Loja Vendas"));
        paymentMethodService.criarPadroesParaLoja(store);

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
            OffsetDateTime.now(),
            null
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
            OffsetDateTime.now(),
            null
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
            OffsetDateTime.now(),
            null
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
            OffsetDateTime.now(),
            null
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
            OffsetDateTime.now(),
            null
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

        assertThat(preview.warnings()).anyMatch(w -> w.contains("estoque negativo"));
    }

    @Test
    void deveListarVendasDaStore() {
        UUID key1 = UUID.randomUUID();
        UUID key2 = UUID.randomUUID();

        saleService.confirmarVenda(new SaleService.ConfirmarVendaRequest(
            store.getId(), key1, "Venda 1",
            List.of(new SaleService.ItemVendaRequest(product1.getId(), BigDecimal.ONE)),
            TransactionSource.MANUAL, null, OffsetDateTime.now(), null
        ));

        saleService.confirmarVenda(new SaleService.ConfirmarVendaRequest(
            store.getId(), key2, "Venda 2",
            List.of(new SaleService.ItemVendaRequest(product2.getId(), BigDecimal.ONE)),
            TransactionSource.MANUAL, null, OffsetDateTime.now(), null
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
            OffsetDateTime.now(),
            null
        );

        assertThatThrownBy(() -> saleService.confirmarVenda(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("desativado");
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
            OffsetDateTime.now(),
            null
        );

        assertThatThrownBy(() -> saleService.confirmarVenda(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("não pertence");
    }

    @Test
    void deveRegistrarVendaComPagamentoETroco() {
        var request = vendaDe(product1, 2);

        Transaction venda = saleService.confirmarVenda(request,
            List.of(new PaymentService.PagamentoRequest(formaDePagamento("Dinheiro"), 15000, 5000)));

        List<Payment> pagamentos = paymentRepository.findByTransaction_Id(venda.getId());
        assertThat(pagamentos).hasSize(1);
        assertThat(pagamentos.get(0).getNetAmountCents()).isEqualTo(10000);
        assertThat(pagamentos.get(0).getChangeCents()).isEqualTo(5000);
    }

    @Test
    void naoGravaNadaSeOPagamentoNaoCobreOTotal() {
        var request = vendaDe(product1, 2);

        assertThatThrownBy(() -> saleService.confirmarVenda(request,
                List.of(new PaymentService.PagamentoRequest(formaDePagamento("PIX"), 5000, 0))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("não cobre o total");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM transactions", Integer.class)).isZero();
        Product semBaixa = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(semBaixa.getStockQuantity()).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    void repetirAMesmaVendaNaoDuplicaOPagamento() {
        var request = vendaDe(product1, 1);
        var pix = List.of(new PaymentService.PagamentoRequest(formaDePagamento("PIX"), 5000, 0));

        Transaction primeira = saleService.confirmarVenda(request, pix);
        Transaction segunda = saleService.confirmarVenda(request, pix);

        assertThat(segunda.getId()).isEqualTo(primeira.getId());
        assertThat(paymentRepository.findByTransaction_Id(primeira.getId())).hasSize(1);
    }

    private SaleService.ConfirmarVendaRequest vendaDe(Product produto, int quantidade) {
        return new SaleService.ConfirmarVendaRequest(
            store.getId(), UUID.randomUUID(), "Venda",
            List.of(new SaleService.ItemVendaRequest(produto.getId(), BigDecimal.valueOf(quantidade))),
            TransactionSource.MANUAL, null, OffsetDateTime.now(), null
        );
    }

    private UUID formaDePagamento(String nome) {
        return paymentMethodRepository.findByStore_IdAndActiveTrue(store.getId()).stream()
            .filter(pm -> pm.getName().equals(nome))
            .findFirst().orElseThrow()
            .getId();
    }
}
