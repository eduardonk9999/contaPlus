package com.contaplus.api.voice;

import com.contaplus.api.expense.ExpenseCategory;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * O que o modelo devolve (structured output). Campos que não se aplicam ao tipo
 * vêm vazios ("" ou 0) em vez de nulos; o VoiceService valida tudo antes de
 * mandar pro app.
 */
public record RespostaModelo(
    @JsonPropertyDescription("VENDA, COMPRA (entrada de estoque), DESPESA (conta paga) ou NAO_ENTENDI")
    TipoLancamento tipo,

    @JsonPropertyDescription("Itens da venda ou compra. Lista vazia para DESPESA e NAO_ENTENDI")
    List<ItemFalado> itens,

    @JsonPropertyDescription("id da forma de pagamento citada, copiado do catálogo; \"\" se não foi dita")
    String formaPagamentoId,

    @JsonPropertyDescription("Quanto o cliente entregou em dinheiro, em centavos, quando a pessoa disser (para o troco); 0 se não disse")
    int valorPagoCentavos,

    @JsonPropertyDescription("Descrição curta da despesa, ex.: \"Conta de luz\"; \"\" se não for DESPESA")
    String descricao,

    @JsonPropertyDescription("Valor da despesa em centavos; 0 se não for DESPESA")
    int valorCentavos,

    @JsonPropertyDescription("Categoria da despesa; OTHER se não for DESPESA ou não se encaixar")
    ExpenseCategory categoria,

    @JsonPropertyDescription("Frase curta em português para o lojista quando faltar informação ou não der pra entender; \"\" caso contrário")
    String observacao
) {

    public record ItemFalado(
        @JsonPropertyDescription("id do produto copiado do catálogo; \"\" se não houver produto correspondente")
        String produtoId,

        @JsonPropertyDescription("Como a pessoa chamou o produto")
        String nomeFalado,

        @JsonPropertyDescription("Quantidade na unidade do produto (ex.: 2, 0.5 kg); 1 se não disse")
        double quantidade,

        @JsonPropertyDescription("Preço unitário em centavos se a pessoa disse; 0 se não disse")
        int valorUnitarioCentavos
    ) {}
}
