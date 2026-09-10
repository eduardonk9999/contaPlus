package com.contaplus.api.store;

import com.contaplus.api.TestcontainersConfiguration;
import com.contaplus.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class StoreServiceTest {

    @Autowired
    StoreService service;

    @Autowired
    StoreRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void deveCriarUmaStore() {
        Store store = service.criar("Loja Teste");

        assertThat(store.getId()).isNotNull();
        assertThat(store.getName()).isEqualTo("Loja Teste");
        assertThat(store.getCurrency()).isEqualTo("BRL");
        assertThat(store.getTimezone()).isEqualTo("America/Sao_Paulo");
    }

    @Test
    void deveBuscarStorePorId() {
        Store created = service.criar("Loja Busca");

        Store found = service.buscarPorId(created.getId());

        assertThat(found.getName()).isEqualTo("Loja Busca");
    }

    @Test
    void deveLancarExcecaoQuandoStoreNaoExiste() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> service.buscarPorId(randomId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Store not found");
    }

    @Test
    void deveListarTodasAsStores() {
        service.criar("Loja 1");
        service.criar("Loja 2");

        List<Store> stores = service.listarTodas();

        assertThat(stores).hasSize(2);
    }

    @Test
    void deveAtualizarStore() {
        Store created = service.criar("Loja Original");

        Store updated = service.atualizar(created.getId(), "Loja Atualizada");

        assertThat(updated.getName()).isEqualTo("Loja Atualizada");
    }

    @Test
    void deveDeletarStore() {
        Store created = service.criar("Loja Deletar");

        service.deletar(created.getId());

        assertThat(repository.findById(created.getId())).isEmpty();
    }
}
