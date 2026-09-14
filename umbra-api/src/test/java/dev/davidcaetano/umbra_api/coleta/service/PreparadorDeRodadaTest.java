package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.service.PreparadorDeRodada.GrupoDeOfertas;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PreparadorDeRodadaTest {

    private final PreparadorDeRodada preparador = new PreparadorDeRodada();

    private OfertaColetada oferta(CodigoLoja loja, String identificadorLoja, String chaveItad) {
        return new OfertaColetada(loja, identificadorLoja, TipoProduto.JOGO, "Jogo", "Acao",
                "https://loja.exemplo/" + identificadorLoja, null, chaveItad,
                1000L, null, null, true, OrigemColeta.STEAM_API, null);
    }

    private static List<GrupoDeOfertas> todosOsGrupos(List<List<GrupoDeOfertas>> chunks) {
        return chunks.stream().flatMap(List::stream).toList();
    }

    @Test
    void deveDescartarSegundaOfertaQuandoDuplicarLojaEIdentificadorNaMesmaRodada() {
        OfertaColetada primeira = oferta(CodigoLoja.STEAM, "730", null);
        OfertaColetada duplicada = oferta(CodigoLoja.STEAM, "730", null);

        List<GrupoDeOfertas> grupos = todosOsGrupos(preparador.preparar(List.of(primeira, duplicada)));

        assertThat(grupos).hasSize(1);
        assertThat(grupos.getFirst().ofertas()).containsExactly(primeira);
    }

    @Test
    void deveAgruparOfertasDeLojasDiferentesQuandoCompartilharChaveItad() {
        OfertaColetada steam = oferta(CodigoLoja.STEAM, "1245620", "gid-elden-ring");
        OfertaColetada nuuvem = oferta(CodigoLoja.NUUVEM, "elden-ring", "gid-elden-ring");

        List<GrupoDeOfertas> grupos = todosOsGrupos(preparador.preparar(List.of(steam, nuuvem)));

        assertThat(grupos).hasSize(1);
        assertThat(grupos.getFirst().ofertas()).containsExactlyInAnyOrder(steam, nuuvem);
    }

    @Test
    void deveCriarGruposDistintosParaOfertasSemChaveItad() {
        OfertaColetada primeira = oferta(CodigoLoja.KABUM, "111", null);
        OfertaColetada segunda = oferta(CodigoLoja.KABUM, "222", null);

        List<GrupoDeOfertas> grupos = todosOsGrupos(preparador.preparar(List.of(primeira, segunda)));

        assertThat(grupos).hasSize(2);
    }

    @Test
    void deveProduzirMaisDeUmChunkQuandoPassarDe500Ofertas() {
        List<OfertaColetada> ofertas = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            ofertas.add(oferta(CodigoLoja.KABUM, "item-" + i, null));
        }

        List<List<GrupoDeOfertas>> chunks = preparador.preparar(ofertas);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.getFirst()).hasSize(500);
        assertThat(chunks.getLast()).hasSize(1);
    }

    @Test
    void naoDeveDividirUmGrupoEntreChunksAindaQuandoSozinhoUltrapassarOTamanhoDoChunk() {
        List<OfertaColetada> ofertas = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            ofertas.add(oferta(CodigoLoja.STEAM, "sku-" + i, "gid-bundle-gigante"));
        }

        List<List<GrupoDeOfertas>> chunks = preparador.preparar(ofertas);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst()).hasSize(1);
        assertThat(chunks.getFirst().getFirst().ofertas()).hasSize(501);
    }

    @Test
    void naoDeveProduzirChunkNenhumQuandoRodadaForVazia() {
        assertThat(preparador.preparar(List.of())).isEmpty();
    }
}
