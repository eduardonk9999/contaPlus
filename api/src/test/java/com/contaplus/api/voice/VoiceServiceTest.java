package com.contaplus.api.voice;

import com.anthropic.models.messages.OutputConfig;
import com.contaplus.api.DatabaseCleaner;
import com.contaplus.api.TestcontainersConfiguration;
import com.contaplus.api.expense.ExpenseCategory;
import com.contaplus.api.payment.PaymentMethod;
import com.contaplus.api.payment.PaymentMethodService;
import com.contaplus.api.product.Product;
import com.contaplus.api.product.ProductRepository;
import com.contaplus.api.product.ProductType;
import com.contaplus.api.product.StockUnit;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class VoiceServiceTest {

    @Autowired
    VoiceService service;

    @MockitoBean
    VoiceInterpreter interpreter;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    PaymentMethodService paymentMethodService;

    @Autowired
    JdbcTemplate jdbc;

    Store store;
    Product coca;
    Product queijo;
    Product embalagem;
    PaymentMethod pix;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.limpar(jdbc);
        store = storeRepository.save(new Store("Mercadinho Voz"));
        paymentMethodService.criarPadroesParaLoja(store);
        pix = paymentMethodService.listarPorStore(store.getId(), false).stream()
            .filter(f -> f.getName().equalsIgnoreCase("Pix"))
            .findFirst().orElseThrow();

        coca = productRepository.save(new Product(store, "Coca-Cola 2L", ProductType.SELLABLE, 700, 1200, StockUnit.UNIT));
        queijo = productRepository.save(new Product(store, "Queijo minas", ProductType.SELLABLE, 3000, 4500, StockUnit.KG));
        embalagem = productRepository.save(new Product(store, "Saco de papel", ProductType.SUPPLY, 10, 0, StockUnit.UNIT));
    }

    @Test
    void requisicaoAoClaudeLevaSchemaEsforcoBaixoEFallback() {
        var contexto = new VoiceInterpreter.Contexto(
            List.of(new VoiceInterpreter.ProdutoCatalogo(coca.getId().toString(), "Coca-Cola 2L", "UNIT")),
            List.of(), OffsetDateTime.now());

        var raw = ClaudeVoiceInterpreter.parametros("claude-opus-5-5", "vendi uma coca", contexto).rawParams();

        var outputConfig = raw.outputConfig().orElseThrow();
        assertThat(outputConfig.format()).isPresent();
        assertThat(outputConfig.effort()).contains(OutputConfig.Effort.LOW);
        assertThat(raw._additionalBodyProperties()).containsKey("fallbacks");
        assertThat(raw.messages().getFirst().content().toString()).contains(coca.getId().toString(), "vendi uma coca");
    }

    @Test
    void vendaUsaPrecoDoCadastroEFormaDePagamentoDaLoja() {
        AtomicReference<VoiceInterpreter.Contexto> contextoEnviado = new AtomicReference<>();
        when(interpreter.interpretar(anyString(), any())).thenAnswer(inv -> {
            contextoEnviado.set(inv.getArgument(1));
            return new RespostaModelo(TipoLancamento.VENDA,
                List.of(new RespostaModelo.ItemFalado(coca.getId().toString(), "coquinha", 2, 999)),
                pix.getId().toString(), 0, "", 0, ExpenseCategory.OTHER, "");
        });

        var resultado = service.interpretar(store.getId(), "vendi duas coquinhas no pix");

        assertThat(resultado.tipo()).isEqualTo(TipoLancamento.VENDA);
        assertThat(resultado.frase()).isEqualTo("vendi duas coquinhas no pix");
        assertThat(resultado.itens()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(coca.getId());
            assertThat(item.quantity()).isEqualByComparingTo("2");
            assertThat(item.unitPriceCents()).isEqualTo(1200);
        });
        assertThat(resultado.paymentMethodId()).isEqualTo(pix.getId());
        assertThat(resultado.mensagem()).isNull();
        assertThat(contextoEnviado.get().produtos()).extracting(VoiceInterpreter.ProdutoCatalogo::nome)
            .contains("Coca-Cola 2L", "Queijo minas");
    }

    @Test
    void idInventadoOuDeOutraLojaViraNaoEncontrado() {
        Store outra = storeRepository.save(new Store("Outra loja"));
        Product deOutraLoja = productRepository.save(new Product(outra, "Guaraná", ProductType.SELLABLE, 500, 900, StockUnit.UNIT));

        when(interpreter.interpretar(anyString(), any())).thenReturn(new RespostaModelo(TipoLancamento.VENDA,
            List.of(
                new RespostaModelo.ItemFalado(deOutraLoja.getId().toString(), "guaraná", 1, 0),
                new RespostaModelo.ItemFalado(UUID.randomUUID().toString(), "pastel", 1, 0),
                new RespostaModelo.ItemFalado(embalagem.getId().toString(), "saquinho", 1, 0)),
            UUID.randomUUID().toString(), 0, "", 0, ExpenseCategory.OTHER, ""));

        var resultado = service.interpretar(store.getId(), "vendi um guaraná, um pastel e um saquinho");

        assertThat(resultado.itens()).isEmpty();
        assertThat(resultado.naoEncontrados()).containsExactly("guaraná", "pastel", "saquinho");
        assertThat(resultado.paymentMethodId()).isNull();
        assertThat(resultado.mensagem()).contains("Não achei no cadastro");
    }

    @Test
    void compraUsaCustoFaladoEAceitaQuantidadeFracionada() {
        when(interpreter.interpretar(anyString(), any())).thenReturn(new RespostaModelo(TipoLancamento.COMPRA,
            List.of(
                new RespostaModelo.ItemFalado(queijo.getId().toString(), "queijo", 2.5, 2800),
                new RespostaModelo.ItemFalado(embalagem.getId().toString(), "saco de papel", 100, 0)),
            "", 0, "", 0, ExpenseCategory.OTHER, ""));

        var resultado = service.interpretar(store.getId(), "comprei 2 kg e meio de queijo a 28 o quilo e 100 sacos");

        assertThat(resultado.tipo()).isEqualTo(TipoLancamento.COMPRA);
        assertThat(resultado.itens()).hasSize(2);
        assertThat(resultado.itens().get(0).quantity()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(resultado.itens().get(0).unitPriceCents()).isEqualTo(2800);
        // insumo entra na compra com o custo do cadastro
        assertThat(resultado.itens().get(1).unitPriceCents()).isEqualTo(10);
    }

    @Test
    void despesaSemValorPedeOValor() {
        when(interpreter.interpretar(anyString(), any())).thenReturn(new RespostaModelo(TipoLancamento.DESPESA,
            List.of(), "", 0, "Conta de luz", 0, ExpenseCategory.ELECTRICITY, ""));

        var resultado = service.interpretar(store.getId(), "paguei a luz");

        assertThat(resultado.tipo()).isEqualTo(TipoLancamento.NAO_ENTENDI);
        assertThat(resultado.mensagem()).isEqualTo("Qual foi o valor da despesa?");
    }

    @Test
    void despesaCompleta() {
        when(interpreter.interpretar(anyString(), any())).thenReturn(new RespostaModelo(TipoLancamento.DESPESA,
            List.of(), "", 0, "Conta de luz", 18000, ExpenseCategory.ELECTRICITY, ""));

        var resultado = service.interpretar(store.getId(), "paguei 180 de luz");

        assertThat(resultado.tipo()).isEqualTo(TipoLancamento.DESPESA);
        assertThat(resultado.despesa().amountCents()).isEqualTo(18000);
        assertThat(resultado.despesa().category()).isEqualTo(ExpenseCategory.ELECTRICITY);
    }

    @Test
    void fraseVaziaNemChegaNoModelo() {
        assertThatThrownBy(() -> service.interpretar(store.getId(), "   "))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(interpreter);
    }
}
