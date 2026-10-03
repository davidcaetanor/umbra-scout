package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JulgamentoDeRodadaTest {

    private final JulgamentoDeRodada julgamento = new JulgamentoDeRodada();

    private static ResultadoColeta resultado(Long declarado, int brutos, int distintos, int elegiveis, int semPreco) {
        return new ResultadoColeta(OrigemColeta.KABUM_API, List.of(), elegiveis, semPreco,
                new Reconciliacao(declarado, brutos, distintos));
    }

    @Test
    void deveSerSaudavelQuandoNenhumaCondicaoDisparar() {
        VereditoRodada veredito = julgamento.julgar(resultado(12L, 12, 12, 12, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
        assertThat(veredito.condicoes()).isEmpty();
    }

    @Test
    void deveRejeitarQuandoSemPrecoPassarDoLimiar() {
        VereditoRodada veredito = julgamento.julgar(resultado(null, 10, 10, 10, 3));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.REJEITADA);
        assertThat(veredito.condicoes()).containsExactly(CondicaoDeDrift.SEM_PRECO_ACIMA_DO_LIMIAR);
    }

    @Test
    void naoDeveDispararSemPrecoQuandoEstiverExatamenteNoLimiar() {
        VereditoRodada veredito = julgamento.julgar(resultado(null, 10, 10, 10, 2));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void naoDeveDispararSemPrecoQuandoElegiveisEstiveremAbaixoDaAmostraMinima() {
        VereditoRodada veredito = julgamento.julgar(resultado(null, 3, 3, 3, 2));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void deveRejeitarQuandoRespostaVierVazia() {
        VereditoRodada veredito = julgamento.julgar(resultado(null, 0, 0, 0, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.REJEITADA);
        assertThat(veredito.condicoes()).containsExactly(CondicaoDeDrift.RESPOSTA_VAZIA);
    }

    @Test
    void deveRejeitarQuandoNenhumBrutoForElegivel() {
        VereditoRodada veredito = julgamento.julgar(resultado(null, 10, 10, 0, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.REJEITADA);
        assertThat(veredito.condicoes()).containsExactly(CondicaoDeDrift.NENHUM_ELEGIVEL);
    }

    @Test
    void naoDeveDispararNenhumElegivelQuandoBrutosEstiveremAbaixoDaAmostraMinima() {
        VereditoRodada veredito = julgamento.julgar(resultado(null, 3, 3, 0, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void deveDegradarQuandoDistintosFicaremAbaixoDoDeclaradoAlemDaMargem() {
        VereditoRodada veredito = julgamento.julgar(resultado(1000L, 900, 900, 900, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.DEGRADADA);
        assertThat(veredito.condicoes()).containsExactly(CondicaoDeDrift.COBERTURA_ABAIXO_DO_DECLARADO);
    }

    @Test
    void naoDeveDispararCoberturaQuandoFaltaEstiverDentroDaMargem() {
        VereditoRodada veredito = julgamento.julgar(resultado(1000L, 990, 990, 990, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void naoDeveDispararCoberturaQuandoFaltaEstiverExatamenteNaMargem() {
        VereditoRodada veredito = julgamento.julgar(resultado(1000L, 980, 980, 980, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void deveDegradarQuandoDuplicataEntrePaginasPassarDaMargem() {
        VereditoRodada veredito = julgamento.julgar(resultado(null, 100, 90, 90, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.DEGRADADA);
        assertThat(veredito.condicoes()).containsExactly(CondicaoDeDrift.DUPLICATA_ENTRE_PAGINAS);
    }

    @Test
    void naoDeveDispararDuplicataQuandoRepeticaoEstiverExatamenteNaMargem() {
        VereditoRodada veredito = julgamento.julgar(resultado(null, 100, 98, 98, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void deveReunirAsDuasCondicoesDeDegradacaoQuandoAmbasDispararem() {
        VereditoRodada veredito = julgamento.julgar(resultado(100L, 100, 90, 90, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.DEGRADADA);
        assertThat(veredito.condicoes()).containsExactlyInAnyOrder(
                CondicaoDeDrift.COBERTURA_ABAIXO_DO_DECLARADO, CondicaoDeDrift.DUPLICATA_ENTRE_PAGINAS);
    }

    @Test
    void deveRejeitarSemPerderCondicaoDeDegradacaoQuandoAmbasDispararem() {
        VereditoRodada veredito = julgamento.julgar(resultado(null, 100, 90, 10, 3));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.REJEITADA);
        assertThat(veredito.condicoes()).containsExactlyInAnyOrder(
                CondicaoDeDrift.SEM_PRECO_ACIMA_DO_LIMIAR, CondicaoDeDrift.DUPLICATA_ENTRE_PAGINAS);
    }
}
