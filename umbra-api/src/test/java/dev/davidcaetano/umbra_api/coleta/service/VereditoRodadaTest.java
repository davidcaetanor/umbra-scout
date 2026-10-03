package dev.davidcaetano.umbra_api.coleta.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VereditoRodadaTest {

    @Test
    void deveExporCondicoesNaOrdemDeDeclaracaoDoEnum() {
        VereditoRodada veredito = new VereditoRodada(Set.of(
                CondicaoDeDrift.DUPLICATA_ENTRE_PAGINAS,
                CondicaoDeDrift.COBERTURA_ABAIXO_DO_DECLARADO,
                CondicaoDeDrift.SEM_PRECO_ACIMA_DO_LIMIAR,
                CondicaoDeDrift.RESPOSTA_VAZIA));

        assertThat(veredito.condicoes()).containsExactly(
                CondicaoDeDrift.RESPOSTA_VAZIA,
                CondicaoDeDrift.SEM_PRECO_ACIMA_DO_LIMIAR,
                CondicaoDeDrift.COBERTURA_ABAIXO_DO_DECLARADO,
                CondicaoDeDrift.DUPLICATA_ENTRE_PAGINAS);
    }

    @Test
    void naoDevePermitirAlterarAsCondicoesDepoisDeCriado() {
        VereditoRodada veredito = new VereditoRodada(Set.of(CondicaoDeDrift.RESPOSTA_VAZIA));

        assertThatThrownBy(() -> veredito.condicoes().add(CondicaoDeDrift.NENHUM_ELEGIVEL))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
