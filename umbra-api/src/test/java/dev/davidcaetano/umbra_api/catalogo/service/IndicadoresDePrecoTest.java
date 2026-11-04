package dev.davidcaetano.umbra_api.catalogo.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndicadoresDePrecoTest {

    @Test
    void variacao_atualAcimaDoMinimo_deveTrazerDiferencaEPercentual() {
        assertThat(IndicadoresDePreco.variacao(11000L, 10000L))
                .isEqualTo(new VariacaoDePreco(11000L, 1000L, new BigDecimal("10.0")));
    }

    @Test
    void variacao_atualIgualAoMinimo_deveSerZero() {
        assertThat(IndicadoresDePreco.variacao(10000L, 10000L))
                .isEqualTo(new VariacaoDePreco(10000L, 0L, new BigDecimal("0.0")));
    }

    @Test
    void variacao_deveArredondarParaUmaCasaMeioParaCima() {
        assertThat(IndicadoresDePreco.variacao(10005L, 10000L).percentualAcimaDoMinimo())
                .isEqualTo(new BigDecimal("0.1"));
        assertThat(IndicadoresDePreco.variacao(10004L, 10000L).percentualAcimaDoMinimo())
                .isEqualTo(new BigDecimal("0.0"));
        assertThat(IndicadoresDePreco.variacao(13333L, 10000L).percentualAcimaDoMinimo())
                .isEqualTo(new BigDecimal("33.3"));
    }

    @Test
    void variacao_minimoZeroEAtualZero_devePercentualZero() {
        assertThat(IndicadoresDePreco.variacao(0L, 0L))
                .isEqualTo(new VariacaoDePreco(0L, 0L, new BigDecimal("0.0")));
    }

    @Test
    void variacao_minimoZeroEAtualPositivo_deveTrazerDiferencaSemPercentual() {
        assertThat(IndicadoresDePreco.variacao(4990L, 0L))
                .isEqualTo(new VariacaoDePreco(4990L, 4990L, null));
    }

    @Test
    void variacao_semPrecoAtual_deveSerTudoNulo() {
        assertThat(IndicadoresDePreco.variacao(null, 10000L)).isEqualTo(new VariacaoDePreco(null, null, null));
    }

    @Test
    void variacao_semMinimo_deveManterSoOPrecoAtual() {
        assertThat(IndicadoresDePreco.variacao(10000L, null)).isEqualTo(new VariacaoDePreco(10000L, null, null));
    }

    @Test
    void faixas_deveDividirOIntervaloEmQuatroPassosIguais() {
        assertThat(IndicadoresDePreco.faixas(List.of(18000L, 10000L, 14000L)))
                .containsExactly(10000L, 12000L, 14000L, 16000L, 18000L);
    }

    @Test
    void faixas_comDivisaoInteira_deveTerOMaiorComoUltimoLimite() {
        assertThat(IndicadoresDePreco.faixas(List.of(10000L, 10007L)))
                .containsExactly(10000L, 10001L, 10002L, 10003L, 10007L);
    }

    @Test
    void faixas_menorIgualAoMaior_deveRepetirOValor() {
        assertThat(IndicadoresDePreco.faixas(List.of(5000L, 5000L)))
                .containsExactly(5000L, 5000L, 5000L, 5000L, 5000L);
    }

    @Test
    void faixas_semValores_deveSerVazia() {
        assertThat(IndicadoresDePreco.faixas(List.of())).isEmpty();
    }
}
