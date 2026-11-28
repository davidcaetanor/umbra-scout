package dev.davidcaetano.umbra_api.catalogo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class CuradoriaDosDestaques {

    private final int destaques;
    private final int porPublicador;

    CuradoriaDosDestaques(int destaques, int porPublicador) {
        if (destaques <= 0 || porPublicador <= 0) {
            throw new IllegalArgumentException("destaques e porPublicador devem ser positivos");
        }
        this.destaques = destaques;
        this.porPublicador = porPublicador;
    }

    List<Long> escolher(List<CandidatoADestaque> candidatosEmOrdem) {
        Set<String> jogosVistos = new HashSet<>();
        Map<String, Integer> porPublicadorVisto = new HashMap<>();
        List<Long> escolhidos = new ArrayList<>();

        for (CandidatoADestaque candidato : candidatosEmOrdem) {
            if (escolhidos.size() == destaques) {
                break;
            }
            if (!candidato.qualificado() || jogosVistos.contains(candidato.chaveDoJogo())) {
                continue;
            }
            String publicador = chaveDoPublicador(candidato.publicador());
            if (publicador != null && porPublicadorVisto.getOrDefault(publicador, 0) >= porPublicador) {
                continue;
            }
            jogosVistos.add(candidato.chaveDoJogo());
            porPublicadorVisto.merge(publicador, 1, Integer::sum);
            escolhidos.add(candidato.produtoId());
        }
        return escolhidos;
    }

    private static String chaveDoPublicador(String publicador) {
        if (publicador == null || publicador.isBlank()) {
            return null;
        }
        return publicador.strip().toLowerCase(Locale.ROOT);
    }
}
