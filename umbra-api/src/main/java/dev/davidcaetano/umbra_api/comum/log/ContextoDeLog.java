package dev.davidcaetano.umbra_api.comum.log;

import org.slf4j.MDC;

import java.util.Map;

public final class ContextoDeLog {

    private ContextoDeLog() {
    }

    public static Map<String, String> copiar() {
        return MDC.getCopyOfContextMap();
    }

    public static void comContexto(Map<String, String> contexto, Runnable tarefa) {
        Map<String, String> anterior = MDC.getCopyOfContextMap();
        aplicar(contexto);
        try {
            tarefa.run();
        } finally {
            aplicar(anterior);
        }
    }

    private static void aplicar(Map<String, String> contexto) {
        if (contexto == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(contexto);
        }
    }
}
