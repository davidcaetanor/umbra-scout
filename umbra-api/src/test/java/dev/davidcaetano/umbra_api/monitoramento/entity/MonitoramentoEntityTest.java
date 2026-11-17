package dev.davidcaetano.umbra_api.monitoramento.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonitoramentoEntityTest {

    private static final OffsetDateTime CRIACAO = OffsetDateTime.of(2026, 11, 17, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime DEPOIS = CRIACAO.plusHours(1);

    @Test
    void deveNascerAtivoSemAvisoEComDatasIguais() {
        MonitoramentoEntity monitoramento = MonitoramentoEntity.novo(1L, 2L, 5000, CRIACAO);

        assertThat(monitoramento.isAtivo()).isTrue();
        assertThat(monitoramento.getUltimoAvisoEm()).isNull();
        assertThat(monitoramento.getUsuarioId()).isEqualTo(1L);
        assertThat(monitoramento.getProdutoId()).isEqualTo(2L);
        assertThat(monitoramento.getPrecoAlvoCentavos()).isEqualTo(5000);
        assertThat(monitoramento.getCriadoEm()).isEqualTo(CRIACAO);
        assertThat(monitoramento.getAtualizadoEm()).isEqualTo(CRIACAO);
    }

    @Test
    void deveRecusarAlvoZeroOuNegativo() {
        assertThatThrownBy(() -> MonitoramentoEntity.novo(1L, 2L, 0, CRIACAO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MonitoramentoEntity.novo(1L, 2L, -1, CRIACAO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveAceitarAlvoDeUmCentavo() {
        assertThat(MonitoramentoEntity.novo(1L, 2L, 1, CRIACAO).getPrecoAlvoCentavos()).isEqualTo(1);
    }

    @Test
    void deveAlterarAlvoEAtualizarAData() {
        MonitoramentoEntity monitoramento = MonitoramentoEntity.novo(1L, 2L, 5000, CRIACAO);

        monitoramento.alterarAlvo(4000, DEPOIS);

        assertThat(monitoramento.getPrecoAlvoCentavos()).isEqualTo(4000);
        assertThat(monitoramento.getAtualizadoEm()).isEqualTo(DEPOIS);
        assertThat(monitoramento.getCriadoEm()).isEqualTo(CRIACAO);
    }

    @Test
    void deveRecusarAlterarParaAlvoZeroSemMudarOEstado() {
        MonitoramentoEntity monitoramento = MonitoramentoEntity.novo(1L, 2L, 5000, CRIACAO);

        assertThatThrownBy(() -> monitoramento.alterarAlvo(0, DEPOIS)).isInstanceOf(IllegalArgumentException.class);
        assertThat(monitoramento.getPrecoAlvoCentavos()).isEqualTo(5000);
        assertThat(monitoramento.getAtualizadoEm()).isEqualTo(CRIACAO);
    }

    @Test
    void devePausarEReativarSemTocarNoUltimoAviso() {
        MonitoramentoEntity monitoramento = MonitoramentoEntity.novo(1L, 2L, 5000, CRIACAO);

        monitoramento.definirAtivo(false, DEPOIS);
        assertThat(monitoramento.isAtivo()).isFalse();
        assertThat(monitoramento.getAtualizadoEm()).isEqualTo(DEPOIS);

        monitoramento.definirAtivo(true, DEPOIS.plusHours(1));
        assertThat(monitoramento.isAtivo()).isTrue();
        assertThat(monitoramento.getUltimoAvisoEm()).isNull();
        assertThat(monitoramento.getAtualizadoEm()).isEqualTo(DEPOIS.plusHours(1));
    }

    @Test
    void deveExigirUsuarioProdutoEData() {
        assertThatThrownBy(() -> MonitoramentoEntity.novo(null, 2L, 5000, CRIACAO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> MonitoramentoEntity.novo(1L, null, 5000, CRIACAO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> MonitoramentoEntity.novo(1L, 2L, 5000, null))
                .isInstanceOf(NullPointerException.class);
    }
}
