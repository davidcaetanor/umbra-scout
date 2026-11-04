package dev.davidcaetano.umbra_api.catalogo.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SerieDoMelhorPrecoTest {

    private static final Instant T0 = Instant.parse("2026-10-01T00:00:00Z");

    @Test
    void umaLoja_deveRepetirOsDegrausDela() {
        List<DegrauDePreco> serie = SerieDoMelhorPreco.calcular(Map.of(
                "steam", List.of(degrau(0, 10000L), degrau(5, 8000L), degrau(9, 9000L))));

        assertThat(serie).containsExactly(degrau(0, 10000L), degrau(5, 8000L), degrau(9, 9000L));
    }

    @Test
    void tresLojasTrocandoDeVencedora_deveSeguirOMenorPrecoACadaMudanca() {
        Map<String, List<DegrauDePreco>> degraus = new LinkedHashMap<>();
        degraus.put("steam", List.of(degrau(0, 10000L), degrau(6, 7000L)));
        degraus.put("nuuvem", List.of(degrau(0, 12000L), degrau(2, 9000L)));
        degraus.put("gog", List.of(degrau(0, 11000L), degrau(4, 8000L), degrau(8, 6500L)));

        List<DegrauDePreco> serie = SerieDoMelhorPreco.calcular(degraus);

        assertThat(serie).containsExactly(
                degrau(0, 10000L), degrau(2, 9000L), degrau(4, 8000L), degrau(6, 7000L), degrau(8, 6500L));
    }

    @Test
    void mudancaQueNaoAlteraOMelhor_naoDeveGerarDegrauRepetido() {
        Map<String, List<DegrauDePreco>> degraus = new LinkedHashMap<>();
        degraus.put("steam", List.of(degrau(0, 5000L)));
        degraus.put("nuuvem", List.of(degrau(0, 9000L), degrau(3, 5000L), degrau(7, 9500L)));

        List<DegrauDePreco> serie = SerieDoMelhorPreco.calcular(degraus);

        assertThat(serie).containsExactly(degrau(0, 5000L));
    }

    @Test
    void lojaQueComecaDepois_naoDeveExistirAntesDoPrimeiroDegrauDela() {
        Map<String, List<DegrauDePreco>> degraus = new LinkedHashMap<>();
        degraus.put("steam", List.of(degrau(0, 10000L)));
        degraus.put("gog", List.of(degrau(5, 4000L)));

        List<DegrauDePreco> serie = SerieDoMelhorPreco.calcular(degraus);

        assertThat(serie).containsExactly(degrau(0, 10000L), degrau(5, 4000L));
    }

    @Test
    void todasIndisponiveisNoMesmoTrecho_deveGerarLacunaEVoltar() {
        Map<String, List<DegrauDePreco>> degraus = new LinkedHashMap<>();
        degraus.put("steam", List.of(degrau(0, 10000L), degrau(3, null), degrau(8, 9500L)));
        degraus.put("nuuvem", List.of(degrau(0, 11000L), degrau(2, null), degrau(9, 9000L)));

        List<DegrauDePreco> serie = SerieDoMelhorPreco.calcular(degraus);

        assertThat(serie).containsExactly(
                degrau(0, 10000L), degrau(3, null), degrau(8, 9500L), degrau(9, 9000L));
    }

    @Test
    void lacunaDeUmaLoja_naoDeveVirarZeroNemDerrubarOMelhorDasOutras() {
        Map<String, List<DegrauDePreco>> degraus = new LinkedHashMap<>();
        degraus.put("kabum", List.of(degrau(0, 30000L), degrau(4, null)));
        degraus.put("steam", List.of(degrau(0, 35000L)));

        List<DegrauDePreco> serie = SerieDoMelhorPreco.calcular(degraus);

        assertThat(serie).containsExactly(degrau(0, 30000L), degrau(4, 35000L));
    }

    @Test
    void primeiroDegrauIndisponivel_deveComecarComLacuna() {
        List<DegrauDePreco> serie = SerieDoMelhorPreco.calcular(Map.of(
                "kabum", List.of(degrau(0, null), degrau(2, 15000L))));

        assertThat(serie).containsExactly(degrau(0, null), degrau(2, 15000L));
    }

    @Test
    void semOfertas_deveDevolverSerieVazia() {
        assertThat(SerieDoMelhorPreco.calcular(Map.<String, List<DegrauDePreco>>of())).isEmpty();
    }

    private static DegrauDePreco degrau(int dia, Long valorCentavos) {
        return new DegrauDePreco(T0.plusSeconds(dia * 86_400L), valorCentavos);
    }
}
