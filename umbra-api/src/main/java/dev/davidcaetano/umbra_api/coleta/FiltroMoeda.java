package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.TreeSet;

@Slf4j
public class FiltroMoeda {
    private static final String MOEDA_ACEITA = "BRL";
    private static final String MOEDA_AUSENTE = "ausente";

    private final Set<String> moedasVistas = new TreeSet<>();

    private int totalDescartado;

    public boolean aceita(String... moedas) {
        for (String moeda : moedas) {
            if (!MOEDA_ACEITA.equals(moeda)) {
                return false;
            }
        }

        return true;
    }

    public void registrarDescarte(String... moedas) {
        for (String moeda : moedas) {
            if (!MOEDA_ACEITA.equals(moeda)) {
                moedasVistas.add(moeda == null ? MOEDA_AUSENTE : moeda);
            }
        }

        totalDescartado++;
    }

    public void logarResumo(OrigemColeta fonte) {
        if (totalDescartado == 0) {
            return;
        }

        log.warn("{}: {} ofertas descartadas por moeda diferente de {} nesta rodada (moedas vistas: {})",
                fonte, totalDescartado, MOEDA_ACEITA, moedasVistas);
    }
}
