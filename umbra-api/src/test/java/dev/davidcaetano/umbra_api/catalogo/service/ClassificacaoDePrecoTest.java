package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.enums.SituacaoDoPreco;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificacaoDePrecoTest {

    private static final Instant AGORA = Instant.parse("2026-11-03T12:00:00Z");

    @Test
    void gratuito_valorZeroSemOriginal_eGratuito() {
        assertThat(ClassificacaoDePreco.gratuito(0, null)).isTrue();
    }

    @Test
    void gratuito_valorZeroComOriginalZero_eGratuito() {
        assertThat(ClassificacaoDePreco.gratuito(0, 0L)).isTrue();
    }

    @Test
    void gratuito_valorZeroComOriginalMaiorQueZero_ePromocaoDe100PorCentoENaoGratuito() {
        assertThat(ClassificacaoDePreco.gratuito(0, 5000L)).isFalse();
    }

    @Test
    void gratuito_valorPositivo_naoEGratuito() {
        assertThat(ClassificacaoDePreco.gratuito(1, null)).isFalse();
    }

    @Test
    void situacao_semPrazoVerificadaHaPouco_eVigente() {
        assertThat(ClassificacaoDePreco.situacao(null, AGORA.minus(Duration.ofHours(2)), AGORA))
                .isEqualTo(SituacaoDoPreco.VIGENTE);
    }

    @Test
    void situacao_semPrazoVerificadaHaExatas24Horas_aindaEVigente() {
        assertThat(ClassificacaoDePreco.situacao(null, AGORA.minus(Duration.ofHours(24)), AGORA))
                .isEqualTo(SituacaoDoPreco.VIGENTE);
    }

    @Test
    void situacao_semPrazoVerificadaHaMaisDe24Horas_estaAtrasada() {
        assertThat(ClassificacaoDePreco.situacao(null, AGORA.minus(Duration.ofHours(24)).minusSeconds(1), AGORA))
                .isEqualTo(SituacaoDoPreco.VERIFICACAO_ATRASADA);
    }

    @Test
    void situacao_prazoNoFuturoComVerificacaoAntiga_estaNoPrazoSemAlarme() {
        assertThat(ClassificacaoDePreco.situacao(AGORA.plus(Duration.ofDays(7)), AGORA.minus(Duration.ofDays(5)), AGORA))
                .isEqualTo(SituacaoDoPreco.PROMOCAO_NO_PRAZO);
    }

    @Test
    void situacao_prazoVencidoSemColetaDepois_vencidaSemColeta() {
        assertThat(ClassificacaoDePreco.situacao(AGORA.minus(Duration.ofSeconds(1)), AGORA.minus(Duration.ofHours(1)), AGORA))
                .isEqualTo(SituacaoDoPreco.PROMOCAO_VENCIDA_SEM_COLETA);
    }

    @Test
    void situacao_prazoExatamenteAgoraSemColetaDepois_vencidaSemColeta() {
        assertThat(ClassificacaoDePreco.situacao(AGORA, AGORA.minus(Duration.ofMinutes(5)), AGORA))
                .isEqualTo(SituacaoDoPreco.PROMOCAO_VENCIDA_SEM_COLETA);
    }

    @Test
    void situacao_prazoVencidoComColetaDepoisDoPrazo_voltaAVigente() {
        assertThat(ClassificacaoDePreco.situacao(AGORA.minus(Duration.ofHours(3)), AGORA.minus(Duration.ofHours(1)), AGORA))
                .isEqualTo(SituacaoDoPreco.VIGENTE);
    }

    @Test
    void situacao_prazoVencidoComColetaDepoisMasAntigaDe24Horas_estaAtrasada() {
        assertThat(ClassificacaoDePreco.situacao(AGORA.minus(Duration.ofDays(3)), AGORA.minus(Duration.ofDays(2)), AGORA))
                .isEqualTo(SituacaoDoPreco.VERIFICACAO_ATRASADA);
    }

    @Test
    void valeParaAviso_soVigenteEPromocaoNoPrazo() {
        assertThat(SituacaoDoPreco.VIGENTE.valeParaAviso()).isTrue();
        assertThat(SituacaoDoPreco.PROMOCAO_NO_PRAZO.valeParaAviso()).isTrue();
        assertThat(SituacaoDoPreco.VERIFICACAO_ATRASADA.valeParaAviso()).isFalse();
        assertThat(SituacaoDoPreco.PROMOCAO_VENCIDA_SEM_COLETA.valeParaAviso()).isFalse();
    }
}
