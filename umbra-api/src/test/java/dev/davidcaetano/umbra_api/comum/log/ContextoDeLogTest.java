package dev.davidcaetano.umbra_api.comum.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextoDeLogTest {

    private static final Map<String, String> DA_REQUISICAO = Map.of("requestId", "id-da-requisicao");
    private static final Map<String, String> COPIADO = Map.of("requestId", "id-copiado");

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void comMdcVazio_aTarefaVeOContexto_eOMdcVoltaVazio() {
        AtomicReference<Map<String, String>> vistoPelaTarefa = new AtomicReference<>();

        ContextoDeLog.comContexto(COPIADO, () -> vistoPelaTarefa.set(MDC.getCopyOfContextMap()));

        assertThat(vistoPelaTarefa.get()).isEqualTo(COPIADO);
        assertThat(mdcAtual()).isEmpty();
    }

    @Test
    void comMdcPreenchido_aTarefaVeOOutroContexto_eOMdcVoltaAoAnterior() {
        MDC.setContextMap(DA_REQUISICAO);
        AtomicReference<Map<String, String>> vistoPelaTarefa = new AtomicReference<>();

        ContextoDeLog.comContexto(COPIADO, () -> vistoPelaTarefa.set(MDC.getCopyOfContextMap()));

        assertThat(vistoPelaTarefa.get()).isEqualTo(COPIADO);
        assertThat(mdcAtual()).isEqualTo(DA_REQUISICAO);
    }

    @Test
    void comContextoNulo_aTarefaRodaSemContexto_eOAnteriorVolta() {
        MDC.setContextMap(DA_REQUISICAO);
        AtomicReference<Map<String, String>> vistoPelaTarefa = new AtomicReference<>();

        ContextoDeLog.comContexto(null, () -> vistoPelaTarefa.set(mdcAtual()));

        assertThat(vistoPelaTarefa.get()).isEmpty();
        assertThat(mdcAtual()).isEqualTo(DA_REQUISICAO);
    }

    @Test
    void tarefaQueLanca_deixaOContextoAnteriorRestaurado() {
        MDC.setContextMap(DA_REQUISICAO);
        IllegalStateException falha = new IllegalStateException("tarefa falhou");

        assertThatThrownBy(() -> ContextoDeLog.comContexto(COPIADO, () -> {
            throw falha;
        })).isSameAs(falha);

        assertThat(mdcAtual()).isEqualTo(DA_REQUISICAO);
    }

    @Test
    void copiar_devolveOContextoDaThreadAtual() {
        MDC.setContextMap(DA_REQUISICAO);

        assertThat(ContextoDeLog.copiar()).isEqualTo(DA_REQUISICAO);
    }

    private static Map<String, String> mdcAtual() {
        Map<String, String> atual = MDC.getCopyOfContextMap();
        return atual == null ? Map.of() : atual;
    }
}
