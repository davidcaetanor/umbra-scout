package dev.davidcaetano.umbra_api.catalogo.entity;

import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity.DadosProduto;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OfertaEntityTest {

    private static final OffsetDateTime PRIMEIRA_COLETA = OffsetDateTime.parse("2026-09-01T09:00:00Z");
    private static final String URL_VALIDA = "https://store.steampowered.com/app/730";

    private final ProdutoEntity produto = ProdutoEntity.novo(TipoProduto.JOGO,
            new DadosProduto("Counter-Strike 2", "Acao", null, null), PRIMEIRA_COLETA);
    private final LojaEntity loja = new LojaEntity();

    @Test
    void deveFalharQuandoProdutoForNulo() {
        assertThatThrownBy(() -> OfertaEntity.nova(null, loja, "app-730", URL_VALIDA, PRIMEIRA_COLETA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("produto");
    }

    @Test
    void deveFalharQuandoLojaForNula() {
        assertThatThrownBy(() -> OfertaEntity.nova(produto, null, "app-730", URL_VALIDA, PRIMEIRA_COLETA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("loja");
    }

    @Test
    void deveFalharQuandoIdentificadorLojaForNulo() {
        assertThatThrownBy(() -> OfertaEntity.nova(produto, loja, null, URL_VALIDA, PRIMEIRA_COLETA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("identificador");
    }

    @Test
    void deveFalharQuandoUrlNaoComecarComHttp() {
        assertThatThrownBy(() -> OfertaEntity.nova(produto, loja, "app-730", "/app/730", PRIMEIRA_COLETA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL");
    }

    @Test
    void deveNascerAtivaQuandoCriarOferta() {
        OfertaEntity oferta = OfertaEntity.nova(produto, loja, "app-730", URL_VALIDA, PRIMEIRA_COLETA);

        assertThat(oferta.isAtiva()).isTrue();
    }

    @Test
    void deveAtualizarUrlQuandoValorMudar() {
        OfertaEntity oferta = OfertaEntity.nova(produto, loja, "app-730", URL_VALIDA, PRIMEIRA_COLETA);
        OffsetDateTime segundaColeta = PRIMEIRA_COLETA.plusHours(6);

        oferta.atualizarUrl(URL_VALIDA + "?prime=1", segundaColeta);

        assertThat(oferta.getUrl()).isEqualTo(URL_VALIDA + "?prime=1");
        assertThat(oferta.getAtualizadoEm()).isEqualTo(segundaColeta);
    }

    @Test
    void deveManterUrlEAtualizadoEmInalteradosQuandoUrlNaoMudar() {
        OfertaEntity oferta = OfertaEntity.nova(produto, loja, "app-730", URL_VALIDA, PRIMEIRA_COLETA);
        OffsetDateTime segundaColeta = PRIMEIRA_COLETA.plusHours(6);

        oferta.atualizarUrl(URL_VALIDA, segundaColeta);

        assertThat(oferta.getUrl()).isEqualTo(URL_VALIDA);
        assertThat(oferta.getAtualizadoEm()).isEqualTo(PRIMEIRA_COLETA);
    }

    @Test
    void deveFalharQuandoAtualizarComUrlRelativa() {
        OfertaEntity oferta = OfertaEntity.nova(produto, loja, "app-730", URL_VALIDA, PRIMEIRA_COLETA);

        assertThatThrownBy(() -> oferta.atualizarUrl("/app/730", PRIMEIRA_COLETA.plusHours(6)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL");
    }

    @Test
    void deveMarcarInativaEAtualizarDataQuandoDesativar() {
        OfertaEntity oferta = OfertaEntity.nova(produto, loja, "app-730", URL_VALIDA, PRIMEIRA_COLETA);
        OffsetDateTime segundaColeta = PRIMEIRA_COLETA.plusHours(6);

        oferta.desativar(segundaColeta);

        assertThat(oferta.isAtiva()).isFalse();
        assertThat(oferta.getAtualizadoEm()).isEqualTo(segundaColeta);
    }

    @Test
    void deveVoltarAAtivaEAtualizarDataQuandoReativar() {
        OfertaEntity oferta = OfertaEntity.nova(produto, loja, "app-730", URL_VALIDA, PRIMEIRA_COLETA);
        oferta.desativar(PRIMEIRA_COLETA.plusHours(6));
        OffsetDateTime terceiraColeta = PRIMEIRA_COLETA.plusHours(12);

        oferta.reativar(terceiraColeta);

        assertThat(oferta.isAtiva()).isTrue();
        assertThat(oferta.getAtualizadoEm()).isEqualTo(terceiraColeta);
    }
}
