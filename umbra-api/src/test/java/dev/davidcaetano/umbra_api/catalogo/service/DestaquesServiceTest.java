package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.dto.response.BuscaProdutosResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.ProdutoResumoResponse;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.catalogo.properties.OfertasProperties;
import dev.davidcaetano.umbra_api.catalogo.repository.QualidadeDoCandidatoProjecao;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DestaquesServiceTest {

    private final ProdutoService produtoService = mock(ProdutoService.class);
    private final ConsultaDoCatalogo consultaDoCatalogo = mock(ConsultaDoCatalogo.class);

    @Test
    void dentroDaValidade_deveCalcularUmaVezSo() {
        responder(jogo(1, "Hades II"));
        DestaquesService destaques = new DestaquesService(produtoService, consultaDoCatalogo, propriedades(Duration.ofMinutes(10), 8));

        destaques.destaques();
        destaques.destaques();

        verify(produtoService, times(1)).buscar(any(), eq(OrdenacaoCatalogo.MELHORES_OFERTAS), anyBoolean(), any(), any());
    }

    @Test
    void validadeZero_deveRecalcularACadaPedido() {
        responder(jogo(1, "Hades II"));
        DestaquesService destaques = new DestaquesService(produtoService, consultaDoCatalogo, propriedades(Duration.ZERO, 8));

        destaques.destaques();
        destaques.destaques();

        verify(produtoService, times(2)).buscar(any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void deveDeixarDeForaQuemNaoPassaOPortaoEUmPorPublicador() {
        responder(jogo(1, "Heroes of Hexaluga"), jogo(2, "Fallalypse"), jogo(3, "Hollow Knight"),
                jogo(4, "Celeste"), jogo(5, "Hades"));
        qualificar(qualidade(1, "Hede", false), qualidade(2, "Hede", true), qualidade(3, "Team Cherry", true),
                qualidade(4, "HEDE", true), qualidade(5, "Supergiant", true));
        DestaquesService destaques = new DestaquesService(produtoService, consultaDoCatalogo,
                propriedades(Duration.ZERO, 8));

        assertThat(destaques.destaques().conteudo()).extracting(ProdutoResumoResponse::id).containsExactly(2L, 3L, 5L);
    }

    @Test
    void semQualidadeConsultada_naoEntraNosDestaques() {
        responder(jogo(1, "Sem consulta"), jogo(2, "Hades"));
        qualificar(qualidade(2, "Supergiant", true));
        DestaquesService destaques = new DestaquesService(produtoService, consultaDoCatalogo,
                propriedades(Duration.ZERO, 8));

        assertThat(destaques.destaques().conteudo()).extracting(ProdutoResumoResponse::id).containsExactly(2L);
    }

    @Test
    void deveManterAPrimeiraEdicaoDeCadaJogoERespeitarOLimite() {
        responder(jogo(1, "Zeta: Ultimate Edition"), jogo(2, "Zeta Deluxe Edition"), jogo(3, "Outro"), jogo(4, "Mais um"));
        DestaquesService destaques = new DestaquesService(produtoService, consultaDoCatalogo, propriedades(Duration.ZERO, 2));

        assertThat(destaques.destaques().conteudo()).extracting(ProdutoResumoResponse::id).containsExactly(1L, 3L);
    }

    private void responder(ProdutoResumoResponse... produtos) {
        when(produtoService.buscar(any(), any(), anyBoolean(), any(), any()))
                .thenReturn(new BuscaProdutosResponse(List.of(produtos), 0, 100, produtos.length, 1, List.of()));
        qualificar(Arrays.stream(produtos).map(produto -> qualidade(produto.id(), null, true))
                .toArray(QualidadeDoCandidatoProjecao[]::new));
    }

    private void qualificar(QualidadeDoCandidatoProjecao... qualidades) {
        when(consultaDoCatalogo.qualidadeDosCandidatos(any())).thenReturn(List.of(qualidades));
    }

    private static QualidadeDoCandidatoProjecao qualidade(long id, String publicador, boolean qualificado) {
        return new QualidadeDoCandidatoProjecao() {
            @Override
            public Long getProdutoId() {
                return id;
            }

            @Override
            public String getPublicador() {
                return publicador;
            }

            @Override
            public boolean isQualificado() {
                return qualificado;
            }
        };
    }

    private static ProdutoResumoResponse jogo(long id, String nome) {
        return new ProdutoResumoResponse(id, nome, TipoProduto.JOGO, null, null, null);
    }

    private static OfertasProperties propriedades(Duration validade, int destaques) {
        return new OfertasProperties(0.7, 11342, destaques, 100, validade,
                new OfertasProperties.Hardware(0.4, 0.3, 50000),
                new OfertasProperties.Qualidade(500, 70, 70, 0.75, 0.5, 1));
    }
}
