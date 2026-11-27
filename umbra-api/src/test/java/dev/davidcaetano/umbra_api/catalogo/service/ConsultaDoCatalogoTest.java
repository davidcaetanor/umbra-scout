package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.CatalogoSobrecarregadoException;
import dev.davidcaetano.umbra_api.catalogo.properties.CatalogoProperties;
import dev.davidcaetano.umbra_api.catalogo.properties.OfertasProperties;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.TransactionTimedOutException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultaDoCatalogoTest {

    private static final Duration TENTAR_DE_NOVO_EM = Duration.ofSeconds(5);

    private final ProdutoRepository produtoRepository = mock(ProdutoRepository.class);
    private final ConsultaDoCatalogo consulta = new ConsultaDoCatalogo(produtoRepository,
            mock(OfertasProperties.class), new CatalogoProperties(3, TENTAR_DE_NOVO_EM));

    @Test
    void consultaCanceladaPeloPrazo_deveVirarCatalogoSobrecarregado() {
        QueryTimeoutException cancelada = new QueryTimeoutException("canceling statement due to statement timeout");
        when(produtoRepository.buscarCatalogo(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(cancelada);

        assertThatThrownBy(() -> consulta.buscar(FiltroDoCatalogo.dosDestaques(), OrdenacaoCatalogo.MELHORES_OFERTAS,
                PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(CatalogoSobrecarregadoException.class,
                        sobrecarga -> assertThat(sobrecarga.tentarDeNovoEm()).isEqualTo(TENTAR_DE_NOVO_EM))
                .hasCause(cancelada);
    }

    @Test
    void prazoDaTransacaoVencido_deveVirarCatalogoSobrecarregado() {
        when(produtoRepository.buscarCatalogo(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new TransactionTimedOutException("Transaction timed out"));

        assertThatThrownBy(() -> consulta.buscar(FiltroDoCatalogo.dosDestaques(), OrdenacaoCatalogo.MELHORES_OFERTAS,
                PageRequest.of(0, 20)))
                .isInstanceOf(CatalogoSobrecarregadoException.class);
    }
}
