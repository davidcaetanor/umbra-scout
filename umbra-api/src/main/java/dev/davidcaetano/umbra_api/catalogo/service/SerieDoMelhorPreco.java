package dev.davidcaetano.umbra_api.catalogo.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public final class SerieDoMelhorPreco {

    private SerieDoMelhorPreco() {
    }
    public static <K> List<DegrauDePreco> calcular(Map<K, List<DegrauDePreco>> degrausPorOferta) {
        List<List<DegrauDePreco>> ofertas = List.copyOf(degrausPorOferta.values());
        int[] degrausJaVigentes = new int[ofertas.size()];
        List<DegrauDePreco> serie = new ArrayList<>();

        for (Instant instante : todosOsInstantes(ofertas)) {
            Long melhor = null;

            for (int i = 0; i < ofertas.size(); i++) {
                List<DegrauDePreco> degraus = ofertas.get(i);
                while (degrausJaVigentes[i] < degraus.size()
                        && !degraus.get(degrausJaVigentes[i]).instante().isAfter(instante)) {
                    degrausJaVigentes[i]++;
                }
                if (degrausJaVigentes[i] > 0) {
                    melhor = menorEntre(melhor, degraus.get(degrausJaVigentes[i] - 1).valorCentavos());
                }
            }

            if (serie.isEmpty() || !Objects.equals(serie.getLast().valorCentavos(), melhor)) {
                serie.add(new DegrauDePreco(instante, melhor));
            }
        }

        return serie;
    }

    private static SortedSet<Instant> todosOsInstantes(List<List<DegrauDePreco>> ofertas) {
        SortedSet<Instant> instantes = new TreeSet<>();
        ofertas.forEach(degraus -> degraus.forEach(degrau -> instantes.add(degrau.instante())));
        return instantes;
    }

    private static Long menorEntre(Long atual, Long candidato) {
        if (candidato == null) {
            return atual;
        }
        return atual == null || candidato < atual ? candidato : atual;
    }
}
