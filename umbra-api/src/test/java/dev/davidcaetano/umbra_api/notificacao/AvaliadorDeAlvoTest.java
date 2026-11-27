package dev.davidcaetano.umbra_api.notificacao;

import dev.davidcaetano.umbra_api.catalogo.enums.SituacaoDoPreco;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvaliadorDeAlvoTest {

    private static final Instant AGORA = Instant.parse("2026-11-27T18:30:00Z");
    private static final long ALVO = 5000;

    private final AvaliadorDeAlvo avaliador = new AvaliadorDeAlvo(Duration.ofHours(24));

    private static OfertaAvaliada vigente(long ofertaId, long valor) {
        return new OfertaAvaliada(ofertaId, valor, SituacaoDoPreco.VIGENTE);
    }

    private static EstadoDoAviso armado() {
        return new EstadoDoAviso(ALVO, null, null, false);
    }

    private static EstadoDoAviso avisado(long preco, Instant em) {
        return new EstadoDoAviso(ALVO, preco, em, false);
    }

    private DecisaoDeAlvo avaliar(EstadoDoAviso estado, OfertaAvaliada... ofertas) {
        return avaliador.avaliar(estado, List.of(ofertas), AGORA);
    }

    @Test
    void naoDeveAvisarComPrecoAcimaDoAlvo() {
        assertThat(avaliar(armado(), vigente(1, 5001))).isInstanceOf(Manter.class);
    }

    @Test
    void deveAvisarComPrecoIgualAoAlvo() {
        assertThat(avaliar(armado(), vigente(1, ALVO)))
                .isEqualTo(new Avisar(MotivoDoAviso.ENTRADA, ALVO, List.of(vigente(1, ALVO))));
    }

    @Test
    void deveAvisarComPrecoAbaixoDoAlvo() {
        assertThat(avaliar(armado(), vigente(1, 4000)))
                .isEqualTo(new Avisar(MotivoDoAviso.ENTRADA, 4000, List.of(vigente(1, 4000))));
    }

    @Test
    void deveAvisarComPrecoQueSoFoiConfirmadoPelaRodada() {
        EstadoDoAviso nuncaAvisadoEmbora = new EstadoDoAviso(ALVO, null, AGORA.minus(Duration.ofDays(3)), false);

        assertThat(avaliar(nuncaAvisadoEmbora, vigente(1, 4000))).isInstanceOf(Avisar.class);
    }

    @Test
    void deveListarTodasAsLojasQueCruzaramNumAvisoSo() {
        DecisaoDeAlvo decisao = avaliar(armado(), vigente(2, 4800), vigente(1, 4500), vigente(3, 6000));

        assertThat(decisao).isEqualTo(new Avisar(MotivoDoAviso.ENTRADA, 4500,
                List.of(vigente(1, 4500), vigente(2, 4800))));
    }

    @Test
    void naoDeveAvisarDeNovoComPrecoQueSegueAbaixoNoDiaSeguinte() {
        EstadoDoAviso avisadoOntem = avisado(4000, AGORA.minus(Duration.ofHours(30)));

        assertThat(avaliar(avisadoOntem, vigente(1, 4000))).isInstanceOf(Manter.class);
    }

    @Test
    void deveAvisarDeNovoQuandoCairAbaixoDoPrecoAvisado() {
        EstadoDoAviso avisadoOntem = avisado(4000, AGORA.minus(Duration.ofHours(30)));

        assertThat(avaliar(avisadoOntem, vigente(1, 3500)))
                .isEqualTo(new Avisar(MotivoDoAviso.QUEDA, 3500, List.of(vigente(1, 3500))));
    }

    @Test
    void naoDeveAvisarQuedaAntesDoTeto() {
        EstadoDoAviso avisadoHaPouco = avisado(4000, AGORA.minus(Duration.ofHours(24)).plusSeconds(1));

        assertThat(avaliar(avisadoHaPouco, vigente(1, 3500))).isInstanceOf(Manter.class);
    }

    @Test
    void deveAvisarQuedaNoTetoExato() {
        EstadoDoAviso avisadoHaUmDia = avisado(4000, AGORA.minus(Duration.ofHours(24)));

        assertThat(avaliar(avisadoHaUmDia, vigente(1, 3500))).isInstanceOf(Avisar.class);
    }

    @Test
    void deveRearmarQuandoOPrecoSobeAcimaDoAlvo() {
        EstadoDoAviso avisadoOntem = avisado(4000, AGORA.minus(Duration.ofHours(30)));

        assertThat(avaliar(avisadoOntem, vigente(1, 5500))).isInstanceOf(Rearmar.class);
    }

    @Test
    void deveAvisarComoEntradaQuandoVoltarAbaixoDepoisDeRearmado() {
        EstadoDoAviso rearmado = new EstadoDoAviso(ALVO, null, AGORA.minus(Duration.ofHours(30)), false);

        assertThat(avaliar(rearmado, vigente(1, 4000)))
                .isEqualTo(new Avisar(MotivoDoAviso.ENTRADA, 4000, List.of(vigente(1, 4000))));
    }

    @Test
    void naoDeveAvisarRecruzamentoAntesDoTeto() {
        EstadoDoAviso rearmadoHaPouco = new EstadoDoAviso(ALVO, null, AGORA.minus(Duration.ofHours(2)), false);

        assertThat(avaliar(rearmadoHaPouco, vigente(1, 4000))).isInstanceOf(Manter.class);
    }

    @Test
    void naoDeveRearmarQuemJaEstaArmado() {
        assertThat(avaliar(armado(), vigente(1, 5500))).isInstanceOf(Manter.class);
    }

    @ParameterizedTest
    @EnumSource(value = SituacaoDoPreco.class, names = {"VERIFICACAO_ATRASADA", "PROMOCAO_VENCIDA_SEM_COLETA"})
    void naoDeveAvisarComPrecoAConfirmar(SituacaoDoPreco aConfirmar) {
        assertThat(avaliar(armado(), new OfertaAvaliada(1, 4000, aConfirmar))).isInstanceOf(Manter.class);
    }

    @Test
    void naoDeveRearmarComPrecoAConfirmarAcimaDoAlvo() {
        EstadoDoAviso avisadoOntem = avisado(4000, AGORA.minus(Duration.ofHours(30)));

        assertThat(avaliar(avisadoOntem, new OfertaAvaliada(1, 9000, SituacaoDoPreco.VERIFICACAO_ATRASADA)))
                .isInstanceOf(Manter.class);
    }

    @Test
    void deveAvisarPelaOfertaValidaIgnorandoAAConfirmarMaisBarata() {
        DecisaoDeAlvo decisao = avaliar(armado(),
                new OfertaAvaliada(1, 1000, SituacaoDoPreco.VERIFICACAO_ATRASADA),
                new OfertaAvaliada(2, 4500, SituacaoDoPreco.PROMOCAO_NO_PRAZO));

        assertThat(decisao).isEqualTo(new Avisar(MotivoDoAviso.ENTRADA, 4500,
                List.of(new OfertaAvaliada(2, 4500, SituacaoDoPreco.PROMOCAO_NO_PRAZO))));
    }

    @Test
    void naoDeveAvisarProdutoForaDeVenda() {
        EstadoDoAviso foraDeVenda = new EstadoDoAviso(ALVO, null, null, true);

        assertThat(avaliar(foraDeVenda, vigente(1, 4000))).isInstanceOf(Manter.class);
    }

    @Test
    void naoDeveAvisarSemOferta() {
        assertThat(avaliar(armado())).isInstanceOf(Manter.class);
    }

    @Test
    void deveRecusarTetoSemDuracao() {
        assertThatThrownBy(() -> new AvaliadorDeAlvo(Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRecusarAvisoSemOferta() {
        assertThatThrownBy(() -> new Avisar(MotivoDoAviso.ENTRADA, 100, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
