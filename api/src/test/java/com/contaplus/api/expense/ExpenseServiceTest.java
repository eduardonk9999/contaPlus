package com.contaplus.api.expense;

import com.contaplus.api.DatabaseCleaner;
import com.contaplus.api.TestcontainersConfiguration;
import com.contaplus.api.store.Store;
import com.contaplus.api.store.StoreRepository;
import com.contaplus.api.transaction.TransactionSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ExpenseServiceTest {

    @Autowired
    ExpenseService service;

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    JdbcTemplate jdbc;

    Store store;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.limpar(jdbc);
        store = storeRepository.save(new Store("Loja Despesas"));
    }

    @Test
    void registraDespesaComAFraseOriginal() {
        Expense despesa = service.registrar(despesa(UUID.randomUUID(), 12000, "paguei 120 de luz"));

        assertThat(despesa.getId()).isNotNull();
        assertThat(despesa.getCategory()).isEqualTo(ExpenseCategory.ELECTRICITY);
        assertThat(despesa.getSource()).isEqualTo(TransactionSource.VOICE);
        assertThat(despesa.getOriginalInput()).isEqualTo("paguei 120 de luz");
    }

    @Test
    void repetirAMesmaChaveNaoDuplica() {
        UUID chave = UUID.randomUUID();

        Expense primeira = service.registrar(despesa(chave, 12000, null));
        Expense segunda = service.registrar(despesa(chave, 12000, null));

        assertThat(segunda.getId()).isEqualTo(primeira.getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expenses", Integer.class)).isEqualTo(1);
    }

    @Test
    void rejeitaValorZerado() {
        assertThatThrownBy(() -> service.registrar(despesa(UUID.randomUUID(), 0, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maior que zero");
    }

    @Test
    void listaSoAsDespesasDoPeriodo() {
        OffsetDateTime agora = OffsetDateTime.now();
        service.registrar(new ExpenseService.RegistrarDespesa(store.getId(), UUID.randomUUID(), "Aluguel",
            150000, ExpenseCategory.RENT, agora.minusDays(40), TransactionSource.MANUAL, null));
        service.registrar(despesa(UUID.randomUUID(), 12000, null));

        var doMes = service.listar(store.getId(), agora.minusDays(30), agora.plusMinutes(1), PageRequest.of(0, 10));

        assertThat(doMes.getContent()).extracting(Expense::getDescription).containsExactly("Conta de luz");
    }

    private ExpenseService.RegistrarDespesa despesa(UUID chave, int valorCentavos, String frase) {
        return new ExpenseService.RegistrarDespesa(store.getId(), chave, "Conta de luz", valorCentavos,
            ExpenseCategory.ELECTRICITY, null, TransactionSource.VOICE, frase);
    }
}
