package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.JogoExternoNaoEncontradoException;
import dev.davidcaetano.umbra_api.catalogo.service.MaterializacaoIndisponivelException;
import dev.davidcaetano.umbra_api.coleta.service.ColetaService;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ItadMaterializacaoDeJogoTetoTest {

    private static final String GID = "018d937f-590c-728b-ac35-38bcff85f086";

    @Test
    void acimaDoTeto_deveRecusarSemChamarOItadNemGravar() {
        ItadBuscaClient itadBuscaClient = mock(ItadBuscaClient.class);
        ColetaService coletaService = mock(ColetaService.class);
        when(itadBuscaClient.buscarInfo(any(UUID.class))).thenReturn(Optional.empty());
        RateLimiterRegistry registro = RateLimiterRegistry.of(RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofMinutes(5))
                .timeoutDuration(Duration.ZERO)
                .build());
        ItadMaterializacaoDeJogo materializacao = new ItadMaterializacaoDeJogo(itadBuscaClient, coletaService, registro);

        assertThatThrownBy(() -> materializacao.materializar(GID)).isInstanceOf(JogoExternoNaoEncontradoException.class);
        assertThatThrownBy(() -> materializacao.materializar(GID))
                .isInstanceOf(MaterializacaoIndisponivelException.class)
                .hasMessageContaining("teto");

        verify(itadBuscaClient, times(1)).buscarInfo(any(UUID.class));
        verify(coletaService, never()).gravarSobDemanda(any());
    }

    @Test
    void dentroDoTeto_naoDeveGravarQuandoOJogoNaoExistir() {
        ItadBuscaClient itadBuscaClient = mock(ItadBuscaClient.class);
        ColetaService coletaService = mock(ColetaService.class);
        when(itadBuscaClient.buscarInfo(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new ItadMaterializacaoDeJogo(itadBuscaClient, coletaService,
                RateLimiterRegistry.ofDefaults()).materializar(GID))
                .isInstanceOf(JogoExternoNaoEncontradoException.class);

        verifyNoInteractions(coletaService);
    }
}
