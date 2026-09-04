package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.LojaEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity.DadosProduto;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ProdutoRepositoryTest extends IntegrationTestBase {

    private static final OffsetDateTime PRIMEIRA_COLETA = OffsetDateTime.parse("2026-09-01T09:00:00Z");
    private static final OffsetDateTime SEGUNDA_COLETA = OffsetDateTime.parse("2026-09-01T15:00:00Z");
    private static final String IDENTIFICADOR = "app-730";
    private static final String URL = "https://store.steampowered.com/app/730";

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private LojaRepository lojaRepository;

    @Autowired
    private EntityManager em;

    private LojaEntity steam;

    @BeforeEach
    void resolverLoja() {
        steam = lojaRepository.findByCodigo(CodigoLoja.STEAM).orElseThrow();
    }

    private ProdutoEntity produtoDaSteam(String nome) {
        return ProdutoEntity.novo(steam, IDENTIFICADOR, TipoProduto.JOGO,
                new DadosProduto(nome, "Acao", URL, null, null), PRIMEIRA_COLETA);
    }

    @Test
    void deveResolverIdDaLojaQuandoBuscarPeloCodigo() {
        LojaEntity terabyte = lojaRepository.findByCodigo(CodigoLoja.TERABYTE).orElseThrow();

        assertThat(terabyte.getId()).isNotNull();
        assertThat(terabyte.getCodigo()).isEqualTo(CodigoLoja.TERABYTE);
        assertThat(terabyte.getId()).isNotEqualTo(steam.getId());
    }

    @Test
    void deveHidratarTodasAsLojasDoSeedQuandoBuscarTodas() {
        assertThat(lojaRepository.findAll())
                .extracting(LojaEntity::getCodigo)
                .containsExactlyInAnyOrder(CodigoLoja.STEAM, CodigoLoja.NUUVEM,
                        CodigoLoja.TERABYTE, CodigoLoja.EPIC);
    }

    @Test
    void deveAtualizarProdutoExistenteQuandoRecoletarMesmaLojaEIdentificador() {
        Long idOriginal = produtoRepository.saveAndFlush(produtoDaSteam("Counter-Strike 2")).getId();
        em.clear();

        ProdutoEntity existente = produtoRepository
                .findByLojaIdAndIdentificadorLoja(steam.getId(), IDENTIFICADOR)
                .orElseThrow();
        existente.atualizarDados(
                new DadosProduto("Counter-Strike 2 Prime", null, URL + "?prime=1", null, "cs2-itad"),
                SEGUNDA_COLETA);
        em.flush();
        em.clear();

        assertThat(produtoRepository.count()).isEqualTo(1);

        ProdutoEntity relido = produtoRepository.findById(idOriginal).orElseThrow();
        assertThat(relido.getNome()).isEqualTo("Counter-Strike 2 Prime");
        assertThat(relido.getUrl()).isEqualTo(URL + "?prime=1");
        assertThat(relido.getChaveItad()).isEqualTo("cs2-itad");
        assertThat(relido.getCategoria()).isEqualTo("Acao");
    }

    @Test
    void deveFalharQuandoProdutoDuplicaMesmaLoja() {
        produtoRepository.saveAndFlush(produtoDaSteam("Counter-Strike 2"));

        ProdutoEntity duplicado = produtoDaSteam("Counter-Strike 2 duplicado");

        assertThatThrownBy(() -> produtoRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_produto_loja");
    }

    @Test
    void devePersistirAlteracaoQuandoMutarEntidadeManagedSemChamarSave() {
        Long id = produtoRepository.saveAndFlush(produtoDaSteam("Nome antigo")).getId();
        em.clear();

        ProdutoEntity managed = produtoRepository.findById(id).orElseThrow();
        managed.atualizarDados(new DadosProduto("Nome novo", null, null, null, null), SEGUNDA_COLETA);

        em.flush();
        em.clear();

        assertThat(produtoRepository.findById(id).orElseThrow().getNome()).isEqualTo("Nome novo");
    }

    @Test
    void deveAvancarAtualizadoEmQuandoUpsertAtualizarProdutoExistente() {
        ProdutoEntity criado = produtoRepository.saveAndFlush(produtoDaSteam("Counter-Strike 2"));
        Long id = criado.getId();

        assertThat(criado.getCriadoEm().toInstant()).isEqualTo(PRIMEIRA_COLETA.toInstant());
        assertThat(criado.getAtualizadoEm().toInstant()).isEqualTo(PRIMEIRA_COLETA.toInstant());
        em.clear();

        ProdutoEntity existente = produtoRepository.findById(id).orElseThrow();
        existente.atualizarDados(new DadosProduto("Counter-Strike 2 Prime", null, null, null, null),
                SEGUNDA_COLETA);
        em.flush();
        em.clear();

        ProdutoEntity relido = produtoRepository.findById(id).orElseThrow();
        assertThat(relido.getCriadoEm().toInstant()).isEqualTo(PRIMEIRA_COLETA.toInstant());
        assertThat(relido.getAtualizadoEm().toInstant()).isEqualTo(SEGUNDA_COLETA.toInstant());
        assertThat(relido.getAtualizadoEm()).isAfter(relido.getCriadoEm());
    }

    @Test
    void deveGravarTipoComoTextoQuandoPersistirProduto() {
        Long id = produtoRepository.saveAndFlush(produtoDaSteam("Counter-Strike 2")).getId();

        Object tipoNaColuna = em.createNativeQuery("SELECT tipo FROM produto WHERE id = :id")
                .setParameter("id", id)
                .getSingleResult();

        assertThat(tipoNaColuna).isEqualTo("JOGO");
    }

    @Test
    void deveRetornarVazioQuandoIdentificadorNaoExistirNaLoja() {
        produtoRepository.saveAndFlush(produtoDaSteam("Counter-Strike 2"));

        assertThat(produtoRepository.findByLojaIdAndIdentificadorLoja(steam.getId(), "app-000"))
                .isEmpty();
    }

    @Test
    void deveManterLinhaNoBancoQuandoDesativarProduto() {
        Long id = produtoRepository.saveAndFlush(produtoDaSteam("Counter-Strike 2")).getId();
        em.clear();

        produtoRepository.findById(id).orElseThrow().desativar(SEGUNDA_COLETA);
        em.flush();
        em.clear();

        assertThat(produtoRepository.count()).isEqualTo(1);

        ProdutoEntity relido = produtoRepository.findById(id).orElseThrow();
        assertThat(relido.isAtivo()).isFalse();
        assertThat(relido.getAtualizadoEm().toInstant()).isEqualTo(SEGUNDA_COLETA.toInstant());
    }

    @Test
    void deveVoltarAAtivoQuandoReativarProdutoDesativado() {
        Long id = produtoRepository.saveAndFlush(produtoDaSteam("Counter-Strike 2")).getId();
        em.clear();

        produtoRepository.findById(id).orElseThrow().desativar(SEGUNDA_COLETA);
        em.flush();
        em.clear();

        OffsetDateTime terceiraColeta = SEGUNDA_COLETA.plusHours(6);
        produtoRepository.findById(id).orElseThrow().reativar(terceiraColeta);
        em.flush();
        em.clear();

        ProdutoEntity relido = produtoRepository.findById(id).orElseThrow();
        assertThat(relido.isAtivo()).isTrue();
        assertThat(relido.getAtualizadoEm().toInstant()).isEqualTo(terceiraColeta.toInstant());
    }

    @Test
    void deveEncontrarProdutoDesativadoQuandoRecoletarMesmoIdentificador() {
        Long id = produtoRepository.saveAndFlush(produtoDaSteam("Counter-Strike 2")).getId();
        em.clear();

        produtoRepository.findById(id).orElseThrow().desativar(SEGUNDA_COLETA);
        em.flush();
        em.clear();

        assertThat(produtoRepository.findByLojaIdAndIdentificadorLoja(steam.getId(), IDENTIFICADOR))
                .isPresent()
                .get()
                .extracting(ProdutoEntity::getId)
                .isEqualTo(id);
    }

}
