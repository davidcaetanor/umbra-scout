package dev.davidcaetano.umbra_api.catalogo.entity;

import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity.DadosProduto;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class ProdutoEntityTest {

    private static final OffsetDateTime PRIMEIRA_COLETA = OffsetDateTime.parse("2026-09-01T09:00:00Z");

    private DadosProduto dados(String nome, String categoria, String imagemUrl, String chaveItad) {
        return new DadosProduto(nome, categoria, imagemUrl, chaveItad);
    }

    @Test
    void deveFalharQuandoNomeTiverApenasEspacos() {
        assertThatThrownBy(() -> ProdutoEntity.novo(TipoProduto.JOGO,
                dados("   ", "Acao", null, null), PRIMEIRA_COLETA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome");
    }

    @Test
    void deveFalharQuandoTipoForNulo() {
        assertThatThrownBy(() -> ProdutoEntity.novo(null,
                dados("Counter-Strike 2", "Acao", null, null), PRIMEIRA_COLETA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tipo");
    }

    @Test
    void deveFalharQuandoNomeForNulo() {
        assertThatThrownBy(() -> ProdutoEntity.novo(TipoProduto.JOGO,
                dados(null, "Acao", null, null), PRIMEIRA_COLETA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("nome");
    }

    @Test
    void deveRemoverEspacosLateraisDoNomeQuandoCriarProduto() {
        ProdutoEntity produto = ProdutoEntity.novo(TipoProduto.JOGO,
                dados("  Counter-Strike 2  ", "Acao", null, null), PRIMEIRA_COLETA);

        assertThat(produto.getNome()).isEqualTo("Counter-Strike 2");
    }

    @Test
    void deveNascerAtivoQuandoCriarProduto() {
        ProdutoEntity produto = ProdutoEntity.novo(TipoProduto.JOGO,
                dados("Counter-Strike 2", "Acao", null, null), PRIMEIRA_COLETA);

        assertThat(produto.isAtivo()).isTrue();
    }

    @Test
    void deveMarcarInativoEAtualizarDataQuandoDesativar() {
        ProdutoEntity produto = ProdutoEntity.novo(TipoProduto.JOGO,
                dados("Counter-Strike 2", "Acao", null, null), PRIMEIRA_COLETA);
        OffsetDateTime segundaColeta = PRIMEIRA_COLETA.plusHours(6);

        produto.desativar(segundaColeta);

        assertThat(produto.isAtivo()).isFalse();
        assertThat(produto.getAtualizadoEm()).isEqualTo(segundaColeta);
        assertThat(produto.getCriadoEm()).isEqualTo(PRIMEIRA_COLETA);
    }

    @Test
    void deveVoltarAAtivoEAtualizarDataQuandoReativar() {
        ProdutoEntity produto = ProdutoEntity.novo(TipoProduto.JOGO,
                dados("Counter-Strike 2", "Acao", null, null), PRIMEIRA_COLETA);
        produto.desativar(PRIMEIRA_COLETA.plusHours(6));
        OffsetDateTime terceiraColeta = PRIMEIRA_COLETA.plusHours(12);

        produto.reativar(terceiraColeta);

        assertThat(produto.isAtivo()).isTrue();
        assertThat(produto.getAtualizadoEm()).isEqualTo(terceiraColeta);
    }

    @Test
    void devePreencherCamposAusentesQuandoCompletarDadosComInformacaoNova() {
        ProdutoEntity produto = ProdutoEntity.novo(TipoProduto.JOGO,
                dados("Counter-Strike 2", null, null, null), PRIMEIRA_COLETA);

        OffsetDateTime segundaColeta = PRIMEIRA_COLETA.plusHours(6);
        produto.completarDadosAusentes(
                dados("Counter-Strike 2 (Nuuvem)", "Acao", "https://img/cs2.jpg", "cs2-itad"),
                segundaColeta);

        assertThat(produto.getCategoria()).isEqualTo("Acao");
        assertThat(produto.getImagemUrl()).isEqualTo("https://img/cs2.jpg");
        assertThat(produto.getChaveItad()).isEqualTo("cs2-itad");
        assertThat(produto.getAtualizadoEm()).isEqualTo(segundaColeta);
    }

    @Test
    void deveManterValoresExistentesEAtualizadoEmInalteradoQuandoNadaEstiverAusente() {
        ProdutoEntity produto = ProdutoEntity.novo(TipoProduto.JOGO,
                dados("Counter-Strike 2", "Acao", null, null), PRIMEIRA_COLETA);

        OffsetDateTime segundaColeta = PRIMEIRA_COLETA.plusHours(6);
        produto.completarDadosAusentes(
                dados("Counter-Strike 2 (Nuuvem)", "Fps", null, null), segundaColeta);

        assertThat(produto.getNome()).isEqualTo("Counter-Strike 2");
        assertThat(produto.getCategoria()).isEqualTo("Acao");
        assertThat(produto.getAtualizadoEm()).isEqualTo(PRIMEIRA_COLETA);
    }

}
