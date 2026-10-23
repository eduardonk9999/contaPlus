package com.contaplus.api.voice;

import com.contaplus.api.expense.ExpenseCategory;
import com.contaplus.api.payment.PaymentMethod;
import com.contaplus.api.payment.PaymentMethodService;
import com.contaplus.api.product.Product;
import com.contaplus.api.product.ProductRepository;
import com.contaplus.api.product.StockUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Interpreta a frase e confere tudo contra o cadastro da loja: o modelo só
 * sugere, quem decide o que vale é o banco. Nada é gravado aqui; o app mostra
 * a confirmação e usa os endpoints normais de venda, compra e despesa.
 */
@Service
public class VoiceService {

    static final int TAMANHO_MAXIMO_FRASE = 500;

    private final VoiceInterpreter interpreter;
    private final ProductRepository productRepository;
    private final PaymentMethodService paymentMethodService;

    VoiceService(VoiceInterpreter interpreter,
                 ProductRepository productRepository,
                 PaymentMethodService paymentMethodService) {
        this.interpreter = interpreter;
        this.productRepository = productRepository;
        this.paymentMethodService = paymentMethodService;
    }

    @Transactional(readOnly = true)
    public Interpretacao interpretar(UUID storeId, String frase) {
        String texto = frase == null ? "" : frase.strip();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Fale ou digite o que aconteceu.");
        }
        if (texto.length() > TAMANHO_MAXIMO_FRASE) {
            throw new IllegalArgumentException("Frase muito longa. Registre um lançamento por vez.");
        }

        Map<String, Product> produtos = productRepository.findByStore_IdAndActiveTrue(storeId).stream()
            .collect(Collectors.toMap(p -> p.getId().toString(), Function.identity()));
        Map<String, PaymentMethod> formas = paymentMethodService.listarPorStore(storeId, false).stream()
            .collect(Collectors.toMap(f -> f.getId().toString(), Function.identity()));

        VoiceInterpreter.Contexto contexto = new VoiceInterpreter.Contexto(
            produtos.values().stream()
                .map(p -> new VoiceInterpreter.ProdutoCatalogo(p.getId().toString(), p.getName(), p.getStockUnit().name()))
                .toList(),
            formas.values().stream()
                .map(f -> new VoiceInterpreter.FormaPagamentoCatalogo(f.getId().toString(), f.getName()))
                .toList(),
            OffsetDateTime.now()
        );

        RespostaModelo resposta = interpreter.interpretar(texto, contexto);
        if (resposta == null || resposta.tipo() == null) {
            return naoEntendi(texto, "Não entendi. Pode repetir?");
        }

        return switch (resposta.tipo()) {
            case VENDA, COMPRA -> itens(texto, resposta, produtos, formas);
            case DESPESA -> despesa(texto, resposta);
            case NAO_ENTENDI -> naoEntendi(texto, observacaoOu(resposta, "Não entendi. Pode repetir?"));
        };
    }

    private Interpretacao itens(String frase, RespostaModelo resposta,
                                Map<String, Product> produtos, Map<String, PaymentMethod> formas) {
        boolean venda = resposta.tipo() == TipoLancamento.VENDA;
        List<ItemInterpretado> itens = new ArrayList<>();
        List<String> naoEncontrados = new ArrayList<>();

        for (RespostaModelo.ItemFalado falado : resposta.itens() == null ? List.<RespostaModelo.ItemFalado>of() : resposta.itens()) {
            Product produto = falado.produtoId() == null ? null : produtos.get(falado.produtoId());
            // produto de insumo não tem preço de venda: não dá pra vender pelo caixa
            if (produto == null || (venda && (produto.getSalePriceCents() == null || produto.getSalePriceCents() <= 0))) {
                naoEncontrados.add(nomeOu(falado.nomeFalado(), produto));
                continue;
            }

            BigDecimal quantidade = falado.quantidade() > 0
                ? BigDecimal.valueOf(falado.quantidade()).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros()
                : BigDecimal.ONE;
            if (produto.getStockUnit() == StockUnit.UNIT && quantidade.stripTrailingZeros().scale() > 0) {
                quantidade = quantidade.setScale(0, RoundingMode.HALF_UP).max(BigDecimal.ONE);
            }

            Integer valorUnitario = venda
                ? produto.getSalePriceCents()
                : (falado.valorUnitarioCentavos() > 0 ? Integer.valueOf(falado.valorUnitarioCentavos()) : produto.getCostPriceCents());

            itens.add(new ItemInterpretado(produto.getId(), produto.getName(), produto.getStockUnit(),
                quantidade, valorUnitario));
        }

        PaymentMethod forma = venda && resposta.formaPagamentoId() != null
            ? formas.get(resposta.formaPagamentoId())
            : null;
        Integer valorPago = venda && resposta.valorPagoCentavos() > 0 ? resposta.valorPagoCentavos() : null;

        String mensagem = observacaoOu(resposta, null);
        if (itens.isEmpty()) {
            mensagem = naoEncontrados.isEmpty()
                ? "Não entendi quais produtos. Pode repetir?"
                : "Não achei no cadastro: " + String.join(", ", naoEncontrados) + ".";
        }

        return new Interpretacao(resposta.tipo(), frase, itens, naoEncontrados,
            forma != null ? forma.getId() : null,
            forma != null ? forma.getName() : null,
            valorPago, null, mensagem);
    }

    private Interpretacao despesa(String frase, RespostaModelo resposta) {
        String descricao = resposta.descricao() == null ? "" : resposta.descricao().strip();
        if (resposta.valorCentavos() <= 0) {
            return naoEntendi(frase, observacaoOu(resposta, "Qual foi o valor da despesa?"));
        }
        if (descricao.isEmpty()) {
            descricao = "Despesa";
        }
        ExpenseCategory categoria = resposta.categoria() != null ? resposta.categoria() : ExpenseCategory.OTHER;

        return new Interpretacao(TipoLancamento.DESPESA, frase, List.of(), List.of(), null, null, null,
            new DespesaInterpretada(descricao, resposta.valorCentavos(), categoria),
            observacaoOu(resposta, null));
    }

    private static Interpretacao naoEntendi(String frase, String mensagem) {
        return new Interpretacao(TipoLancamento.NAO_ENTENDI, frase, List.of(), List.of(), null, null, null, null, mensagem);
    }

    private static String observacaoOu(RespostaModelo resposta, String padrao) {
        return resposta.observacao() == null || resposta.observacao().isBlank() ? padrao : resposta.observacao().strip();
    }

    private static String nomeOu(String nomeFalado, Product produto) {
        if (nomeFalado != null && !nomeFalado.isBlank()) {
            return nomeFalado.strip();
        }
        return produto != null ? produto.getName() : "produto";
    }

    public record Interpretacao(
        TipoLancamento tipo,
        String frase,
        List<ItemInterpretado> itens,
        List<String> naoEncontrados,
        UUID paymentMethodId,
        String paymentMethodName,
        Integer valorPagoCentavos,
        DespesaInterpretada despesa,
        String mensagem
    ) {}

    /** Em VENDA, unitPriceCents é o preço de venda; em COMPRA, o custo. */
    public record ItemInterpretado(
        UUID productId,
        String productName,
        StockUnit stockUnit,
        BigDecimal quantity,
        Integer unitPriceCents
    ) {}

    public record DespesaInterpretada(
        String description,
        Integer amountCents,
        ExpenseCategory category
    ) {}
}
