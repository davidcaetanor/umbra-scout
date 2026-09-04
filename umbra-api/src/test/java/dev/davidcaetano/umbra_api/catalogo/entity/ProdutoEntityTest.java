package dev.davidcaetano.umbra_api.catalogo.entity;

import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity.DadosProduto;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class ProdutoEntityTest {

    private static final OffsetDateTime PRIMEIRA_COLETA = OffsetDateTime.parse("2026-09-01T09:00:00Z");
    private static final String URL_VALIDA = "https://store.steampowered.com/app/730";

    private final LojaEntity loja = new LojaEntity();

    private DadosProduto dados(String nome, String url) {
        return new DadosProduto(nome, "Acao", url, null, null);
    }

    @Test
    void deveFalharQuandoNomeTiverApenasEspacos() {
        assertThatThrownBy(() -> ProdutoEntity.novo(loja, "app-730", TipoProduto.JOGO,
                dados("   ", URL_VALIDA), PRIMEIRA_COLETA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome");
    }

    @Test
    void deveFalharQuandoUrlNaoComecarComHttp() {
        assertThatThrownBy(() -> ProdutoEntity.novo(loja, "app-730", TipoProduto.JOGO,
                dados("Counter-Strike 2", "/app/730"), PRIMEIRA_COLETA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL");
    }

    @Test
    void deveFalharQuandoLojaForNula() {
        assertThatThrownBy(() -> ProdutoEntity.novo(null, "app-730", TipoProduto.JOGO,
                dados("Counter-Strike 2", URL_VALIDA), PRIMEIRA_COLETA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("loja");
    }

    @Test
    void deveRemoverEspacosLateraisDoNomeQuandoCriarProduto() {
        ProdutoEntity produto = ProdutoEntity.novo(loja, "app-730", TipoProduto.JOGO,
                dados("  Counter-Strike 2  ", URL_VALIDA), PRIMEIRA_COLETA);

        assertThat(produto.getNome()).isEqualTo("Counter-Strike 2");
    }

    @Test
    void devePreservarValorAtualQuandoAtualizarComCampoVazio() {
        ProdutoEntity produto = ProdutoEntity.novo(loja, "app-730", TipoProduto.JOGO,
                dados("Counter-Strike 2", URL_VALIDA), PRIMEIRA_COLETA);

        OffsetDateTime segundaColeta = PRIMEIRA_COLETA.plusHours(6);
        produto.atualizarDados(new DadosProduto("   ", null, null, null, "cs2-itad"), segundaColeta);

        assertThat(produto.getNome()).isEqualTo("Counter-Strike 2");
        assertThat(produto.getCategoria()).isEqualTo("Acao");
        assertThat(produto.getChaveItad()).isEqualTo("cs2-itad");
        assertThat(produto.getAtualizadoEm()).isEqualTo(segundaColeta);
    }

    @Test
    void deveFalharQuandoAtualizarComUrlRelativa() {
        ProdutoEntity produto = ProdutoEntity.novo(loja, "app-730", TipoProduto.JOGO,
                dados("Counter-Strike 2", URL_VALIDA), PRIMEIRA_COLETA);

        assertThatThrownBy(() -> produto.atualizarDados(
                new DadosProduto(null, null, "/app/730", null, null), PRIMEIRA_COLETA.plusHours(6)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL");
    }
}
