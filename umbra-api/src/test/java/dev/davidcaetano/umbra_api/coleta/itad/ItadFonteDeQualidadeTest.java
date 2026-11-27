package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.service.QualidadeDoJogo;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadAvaliacaoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadEmpresaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadEstatisticasResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoInfoResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItadFonteDeQualidadeTest {

    private static final UUID GID = UUID.fromString("018d937f-11e3-72e7-9b44-7a2a5c3b0c9b");

    private final ItadClient itadClient = mock(ItadClient.class);
    private final ItadFonteDeQualidade fonte = new ItadFonteDeQualidade(itadClient);

    @Test
    void deveLerSteamAMaiorNotaDaCriticaListaDeDesejosEPublicador() {
        responder(List.of(
                        new ItadAvaliacaoResponse(96, "Steam", 489363),
                        new ItadAvaliacaoResponse(87, "Metascore", 27),
                        new ItadAvaliacaoResponse(90, "Metacritic User Score", 6490),
                        new ItadAvaliacaoResponse(89, "OpenCritic", 76)),
                new ItadEstatisticasResponse(19727),
                List.of(new ItadEmpresaResponse("  Team Cherry "), new ItadEmpresaResponse("Outra")));

        assertThat(fonte.consultar(GID.toString()))
                .contains(new QualidadeDoJogo(96, 489363, 89, 76, 19727, "Team Cherry"));
    }

    @Test
    void semAvaliacaoNemEstatistica_deveDevolverOsCamposVazios() {
        responder(null, null, null);

        assertThat(fonte.consultar(GID.toString())).contains(QualidadeDoJogo.semAvaliacao());
    }

    @Test
    void soComSteam_naoInventaNotaDaCritica() {
        responder(List.of(new ItadAvaliacaoResponse(35, "Steam", 20)), new ItadEstatisticasResponse(82),
                List.of(new ItadEmpresaResponse("Hede")));

        assertThat(fonte.consultar(GID.toString()))
                .contains(new QualidadeDoJogo(35, 20, null, null, 82, "Hede"));
    }

    @Test
    void jogoQueOItadNaoConhece_deveVirVazio() {
        when(itadClient.buscarInfo(GID)).thenReturn(Optional.empty());

        assertThat(fonte.consultar(GID.toString())).isEmpty();
    }

    @Test
    void publicadorMaiorQueAColuna_deveSerCortado() {
        responder(null, null, List.of(new ItadEmpresaResponse("x".repeat(250))));

        assertThat(fonte.consultar(GID.toString())).get()
                .extracting(QualidadeDoJogo::publicador)
                .asString()
                .hasSize(200);
    }

    private void responder(List<ItadAvaliacaoResponse> avaliacoes, ItadEstatisticasResponse estatisticas,
                           List<ItadEmpresaResponse> publicadores) {
        when(itadClient.buscarInfo(GID)).thenReturn(Optional.of(
                new ItadJogoInfoResponse(GID, "Jogo", "game", null, avaliacoes, estatisticas, publicadores)));
    }
}
