package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.coleta.Cobertura;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties.Limiares;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties.Sobrescrita;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JulgamentoDeRodadaTest {

    private static final Limiares LIMIARES_DE_HOJE = new Limiares(10, 20, 2, 2);

    private final JulgamentoDeRodada julgamento = julgamentoCom(Map.of());

    private static JulgamentoDeRodada julgamentoCom(Map<OrigemColeta, Sobrescrita> porFonte) {
        return new JulgamentoDeRodada(new AlarmeColetaProperties(LIMIARES_DE_HOJE, porFonte));
    }

    private static ResultadoColeta resultado(OrigemColeta fonte, Long declarado, int brutos, int distintos,
                                             int elegiveis, int semPreco) {
        return new ResultadoColeta(fonte, List.of(), elegiveis, semPreco,
                new Reconciliacao(declarado, brutos, distintos), Cobertura.amostra());
    }

    @Test
    void deveSerSaudavelQuandoNenhumaCondicaoDisparar() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, 12L, 12, 12, 12, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
        assertThat(veredito.condicoes()).isEmpty();
    }

    @Test
    void deveRejeitarQuandoSemPrecoPassarDoLimiar() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, null, 10, 10, 10, 3));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.REJEITADA);
        assertThat(veredito.condicoes()).containsExactly(CondicaoDeDrift.SEM_PRECO_ACIMA_DO_LIMIAR);
    }

    @Test
    void naoDeveDispararSemPrecoQuandoEstiverExatamenteNoLimiar() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, null, 10, 10, 10, 2));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void naoDeveDispararSemPrecoQuandoElegiveisEstiveremAbaixoDaAmostraMinima() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, null, 3, 3, 3, 2));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void deveRejeitarQuandoRespostaVierVazia() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, null, 0, 0, 0, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.REJEITADA);
        assertThat(veredito.condicoes()).containsExactly(CondicaoDeDrift.RESPOSTA_VAZIA);
    }

    @Test
    void deveRejeitarQuandoNenhumBrutoForElegivel() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, null, 10, 10, 0, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.REJEITADA);
        assertThat(veredito.condicoes()).containsExactly(CondicaoDeDrift.NENHUM_ELEGIVEL);
    }

    @Test
    void naoDeveDispararNenhumElegivelQuandoBrutosEstiveremAbaixoDaAmostraMinima() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, null, 3, 3, 0, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void deveDegradarQuandoDistintosFicaremAbaixoDoDeclaradoAlemDaMargem() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, 1000L, 900, 900, 900, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.DEGRADADA);
        assertThat(veredito.condicoes()).containsExactly(CondicaoDeDrift.COBERTURA_ABAIXO_DO_DECLARADO);
    }

    @Test
    void naoDeveDispararCoberturaQuandoFaltaEstiverDentroDaMargem() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, 1000L, 990, 990, 990, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void naoDeveDispararCoberturaQuandoFaltaEstiverExatamenteNaMargem() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, 1000L, 980, 980, 980, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void deveDegradarQuandoDuplicataEntrePaginasPassarDaMargem() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, null, 100, 90, 90, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.DEGRADADA);
        assertThat(veredito.condicoes()).containsExactly(CondicaoDeDrift.DUPLICATA_ENTRE_PAGINAS);
    }

    @Test
    void naoDeveDispararDuplicataQuandoRepeticaoEstiverExatamenteNaMargem() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, null, 100, 98, 98, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void deveReunirAsDuasCondicoesDeDegradacaoQuandoAmbasDispararem() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, 100L, 100, 90, 90, 0));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.DEGRADADA);
        assertThat(veredito.condicoes()).containsExactlyInAnyOrder(
                CondicaoDeDrift.COBERTURA_ABAIXO_DO_DECLARADO, CondicaoDeDrift.DUPLICATA_ENTRE_PAGINAS);
    }

    @Test
    void deveRejeitarSemPerderCondicaoDeDegradacaoQuandoAmbasDispararem() {
        VereditoRodada veredito = julgamento.julgar(resultado(OrigemColeta.KABUM_API, null, 100, 90, 10, 3));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.REJEITADA);
        assertThat(veredito.condicoes()).containsExactlyInAnyOrder(
                CondicaoDeDrift.SEM_PRECO_ACIMA_DO_LIMIAR, CondicaoDeDrift.DUPLICATA_ENTRE_PAGINAS);
    }

    @Test
    void deveAplicarAMargemDeDuplicataSobrescritaSoNaFonteQueADeclarou() {
        JulgamentoDeRodada julgamentoComSteamSobrescrita = julgamentoCom(
                Map.of(OrigemColeta.STEAM_API, new Sobrescrita(null, null, null, 10)));

        VereditoRodada vereditoSteam = julgamentoComSteamSobrescrita.julgar(
                resultado(OrigemColeta.STEAM_API, null, 10, 9, 9, 0));
        VereditoRodada vereditoKabum = julgamentoComSteamSobrescrita.julgar(
                resultado(OrigemColeta.KABUM_API, null, 10, 9, 9, 0));

        assertThat(vereditoSteam.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
        assertThat(vereditoKabum.situacao()).isEqualTo(SituacaoRodada.DEGRADADA);
        assertThat(vereditoKabum.condicoes()).containsExactly(CondicaoDeDrift.DUPLICATA_ENTRE_PAGINAS);
    }

    @Test
    void deveAplicarAAmostraMinimaSobrescritaSoNaFonteQueADeclarou() {
        JulgamentoDeRodada julgamentoComSteamSobrescrita = julgamentoCom(
                Map.of(OrigemColeta.STEAM_API, new Sobrescrita(5, null, null, null)));

        VereditoRodada vereditoSteam = julgamentoComSteamSobrescrita.julgar(
                resultado(OrigemColeta.STEAM_API, null, 9, 9, 9, 3));
        VereditoRodada vereditoKabum = julgamentoComSteamSobrescrita.julgar(
                resultado(OrigemColeta.KABUM_API, null, 9, 9, 9, 3));

        assertThat(vereditoSteam.situacao()).isEqualTo(SituacaoRodada.REJEITADA);
        assertThat(vereditoSteam.condicoes()).containsExactly(CondicaoDeDrift.SEM_PRECO_ACIMA_DO_LIMIAR);
        assertThat(vereditoKabum.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }
}
