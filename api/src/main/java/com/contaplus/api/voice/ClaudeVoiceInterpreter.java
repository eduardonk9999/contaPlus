package com.contaplus.api.voice;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.anthropic.models.messages.TextBlockParam;
import com.contaplus.api.expense.ExpenseCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

class ClaudeVoiceInterpreter implements VoiceInterpreter {

    private static final Logger log = LoggerFactory.getLogger(ClaudeVoiceInterpreter.class);

    private static final String SYSTEM_PROMPT = """
        Você é o Conta+, assistente de um pequeno comércio brasileiro (mercadinho, padaria, \
        lanchonete, loja de bairro). O lojista fala ou digita uma frase curta e você \
        transforma em um lançamento.

        Tipos:
        - VENDA: o lojista vendeu algo para um cliente ("vendi 2 cocas no pix", "saiu um pão e um leite").
        - COMPRA: entrada de mercadoria no estoque, comprada de fornecedor ("comprei 10 caixas de leite \
        a 4 e 50 cada", "chegou 5 kg de queijo").
        - DESPESA: conta ou gasto da loja que não é mercadoria para revenda ("paguei 180 de luz", \
        "aluguel 1500", "gasolina da entrega 60 reais").
        - NAO_ENTENDI: a frase não é nenhum desses ou falta o essencial (ex.: despesa sem valor).

        Regras:
        - Produtos e formas de pagamento só podem ser referenciados pelo id do catálogo enviado. \
        Considere apelidos, diminutivos, plural e erros de transcrição ("coquinha" = Coca-Cola). \
        Se nenhum produto corresponder, use produtoId "" e mantenha nomeFalado.
        - Valores em reais viram centavos: "4 e 50" = 450, "dez conto" = 1000, "1,5 mil" = 150000.
        - Quantidade vem na unidade do produto; "meio quilo" = 0.5. Sem quantidade dita, use 1.
        - Em VENDA, "o cliente me deu 50" ou "pagou com 50" é valorPagoCentavos (para o troco).
        - Não invente forma de pagamento nem preço que a pessoa não disse.
        - observacao é uma frase curta e amigável só quando faltar algo ou não der pra entender.
        """;

    private final AnthropicClient client;
    private final String model;

    ClaudeVoiceInterpreter(String apiKey, String model) {
        this.client = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .timeout(Duration.ofSeconds(30))
            .maxRetries(2)
            .build();
        this.model = model;
    }

    static StructuredMessageCreateParams<RespostaModelo> parametros(String model, String frase, Contexto contexto) {
        MessageCreateParams base = MessageCreateParams.builder()
            .model(model)
            .maxTokens(4000L)
            .systemOfTextBlockParams(List.of(TextBlockParam.builder().text(SYSTEM_PROMPT).build()))
            .outputConfig(RespostaModelo.class, JsonSchemaLocalValidation.YES)
            // se o classificador de segurança recusar, o próprio servidor tenta outro modelo
            .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .addUserMessage(mensagem(frase, contexto))
            .build()
            .rawParams();

        // o builder tipado só gera o schema; o esforço entra no mesmo output_config.
        // Extração curta: esforço baixo responde mais rápido e gasta menos.
        OutputConfig comEsforco = base.outputConfig().orElseThrow().toBuilder()
            .effort(OutputConfig.Effort.LOW)
            .build();
        return new StructuredMessageCreateParams<>(RespostaModelo.class,
            base.toBuilder().outputConfig(comEsforco).build());
    }

    @Override
    public RespostaModelo interpretar(String frase, Contexto contexto) {
        StructuredMessage<RespostaModelo> resposta;
        try {
            resposta = client.messages().create(parametros(model, frase, contexto));
        } catch (AnthropicException e) {
            log.warn("Falha ao interpretar frase com o Claude: {}", e.getMessage());
            throw new VoiceUnavailableException("Não consegui entender agora. Tente de novo em instantes.", e);
        }

        if (resposta.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            return naoEntendi("Não consegui registrar isso. Tente dizer de outro jeito.");
        }

        return resposta.content().stream()
            .flatMap(bloco -> bloco.text().stream())
            .map(StructuredTextBlock::text)
            .findFirst()
            .orElseGet(() -> naoEntendi("Não entendi. Pode repetir?"));
    }

    private static String mensagem(String frase, Contexto contexto) {
        String produtos = contexto.produtos().stream()
            .map(p -> p.id() + " | " + p.nome() + " | " + p.unidade())
            .collect(Collectors.joining("\n"));
        String formas = contexto.formasPagamento().stream()
            .map(f -> f.id() + " | " + f.nome())
            .collect(Collectors.joining("\n"));

        return """
            <catalogo_produtos formato="id | nome | unidade">
            %s
            </catalogo_produtos>

            <formas_pagamento formato="id | nome">
            %s
            </formas_pagamento>

            Agora: %s

            <frase>%s</frase>
            """.formatted(
                produtos.isEmpty() ? "(nenhum produto cadastrado)" : produtos,
                formas.isEmpty() ? "(nenhuma)" : formas,
                contexto.agora().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                frase
            );
    }

    private static RespostaModelo naoEntendi(String observacao) {
        return new RespostaModelo(TipoLancamento.NAO_ENTENDI, List.of(), "", 0, "", 0,
            ExpenseCategory.OTHER, observacao);
    }
}
