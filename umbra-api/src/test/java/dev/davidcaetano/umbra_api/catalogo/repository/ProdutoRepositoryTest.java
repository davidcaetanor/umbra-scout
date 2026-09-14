package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity.DadosProduto;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ProdutoRepositoryTest extends IntegrationTestBase {

    private static final OffsetDateTime PRIMEIRA_COLETA = OffsetDateTime.parse("2026-09-01T09:00:00Z");

    @Autowired
    private ProdutoRepository produtoRepository;

    @Test
    void deveEncontrarProdutoQuandoChaveItadExistir() {
        ProdutoEntity produto = produtoRepository.saveAndFlush(ProdutoEntity.novo(TipoProduto.JOGO,
                new DadosProduto("Counter-Strike 2", "Acao", null, "cs2-itad"), PRIMEIRA_COLETA));

        assertThat(produtoRepository.findByChaveItad("cs2-itad"))
                .isPresent()
                .get()
                .extracting(ProdutoEntity::getId)
                .isEqualTo(produto.getId());
    }

    @Test
    void deveRetornarVazioQuandoChaveItadNaoExistir() {
        produtoRepository.saveAndFlush(ProdutoEntity.novo(TipoProduto.JOGO,
                new DadosProduto("Counter-Strike 2", "Acao", null, "cs2-itad"), PRIMEIRA_COLETA));

        assertThat(produtoRepository.findByChaveItad("nao-existe")).isEmpty();
    }

    @Test
    void deveAceitarVariosProdutosSemChaveItad() {
        produtoRepository.saveAndFlush(ProdutoEntity.novo(TipoProduto.HARDWARE,
                new DadosProduto("SSD Fantasma", "SSD", null, null), PRIMEIRA_COLETA));
        produtoRepository.saveAndFlush(ProdutoEntity.novo(TipoProduto.HARDWARE,
                new DadosProduto("SSD Fantasma 2", "SSD", null, null), PRIMEIRA_COLETA));

        assertThat(produtoRepository.count()).isEqualTo(2);
    }

    @Test
    void deveEncontrarPorChaveItadDepoisQueOGidForCarimbadoEmProdutoCriadoSemEle() {
        String gid = "018d0000-0000-7000-8000-000000000002";

        Long id = produtoRepository.saveAndFlush(ProdutoEntity.novo(TipoProduto.JOGO,
                new DadosProduto("Counter-Strike 2", "Acao", null, null), PRIMEIRA_COLETA)).getId();

        assertThat(produtoRepository.findByChaveItad(gid)).isEmpty();

        ProdutoEntity managed = produtoRepository.findById(id).orElseThrow();
        managed.completarDadosAusentes(
                new DadosProduto(null, null, null, gid), PRIMEIRA_COLETA.plusHours(6));
        produtoRepository.flush();

        assertThat(produtoRepository.findByChaveItad(gid))
                .isPresent()
                .get()
                .extracting(ProdutoEntity::getId)
                .isEqualTo(id);
    }
}
