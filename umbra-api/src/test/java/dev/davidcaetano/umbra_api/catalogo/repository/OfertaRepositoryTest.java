package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.LojaEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.OfertaEntity;
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
class OfertaRepositoryTest extends IntegrationTestBase {

    private static final OffsetDateTime PRIMEIRA_COLETA = OffsetDateTime.parse("2026-09-01T09:00:00Z");
    private static final OffsetDateTime SEGUNDA_COLETA = OffsetDateTime.parse("2026-09-01T15:00:00Z");
    private static final String IDENTIFICADOR = "app-730";
    private static final String URL = "https://store.steampowered.com/app/730";

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private LojaRepository lojaRepository;

    @Autowired
    private EntityManager em;

    private LojaEntity steam;
    private ProdutoEntity produto;

    @BeforeEach
    void preparar() {
        steam = lojaRepository.findByCodigo(CodigoLoja.STEAM).orElseThrow();
        produto = produtoRepository.saveAndFlush(ProdutoEntity.novo(TipoProduto.JOGO,
                new DadosProduto("Counter-Strike 2", "Acao", null, null), PRIMEIRA_COLETA));
    }

    private OfertaEntity ofertaDaSteam() {
        return OfertaEntity.nova(produto, steam, IDENTIFICADOR, URL, PRIMEIRA_COLETA);
    }

    @Test
    void deveFalharQuandoOfertaDuplicaMesmaLoja() {
        ofertaRepository.saveAndFlush(ofertaDaSteam());

        OfertaEntity duplicada = OfertaEntity.nova(produto, steam, IDENTIFICADOR, URL + "?dup=1", PRIMEIRA_COLETA);

        assertThatThrownBy(() -> ofertaRepository.saveAndFlush(duplicada))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_oferta_loja");
    }

    @Test
    void devePersistirAlteracaoQuandoMutarEntidadeManagedSemChamarSave() {
        Long id = ofertaRepository.saveAndFlush(ofertaDaSteam()).getId();
        em.clear();

        OfertaEntity managed = ofertaRepository.findById(id).orElseThrow();
        managed.atualizarUrl(URL + "?prime=1", SEGUNDA_COLETA);

        em.flush();
        em.clear();

        assertThat(ofertaRepository.findById(id).orElseThrow().getUrl()).isEqualTo(URL + "?prime=1");
    }

    @Test
    void deveRetornarVazioQuandoIdentificadorNaoExistirNaLoja() {
        ofertaRepository.saveAndFlush(ofertaDaSteam());

        assertThat(ofertaRepository.findByLojaIdAndIdentificadorLoja(steam.getId(), "app-000"))
                .isEmpty();
    }

    @Test
    void deveManterLinhaNoBancoQuandoDesativarOferta() {
        Long id = ofertaRepository.saveAndFlush(ofertaDaSteam()).getId();
        em.clear();

        ofertaRepository.findById(id).orElseThrow().desativar(SEGUNDA_COLETA);
        em.flush();
        em.clear();

        assertThat(ofertaRepository.count()).isEqualTo(1);

        OfertaEntity relida = ofertaRepository.findById(id).orElseThrow();
        assertThat(relida.isAtiva()).isFalse();
        assertThat(relida.getAtualizadoEm().toInstant()).isEqualTo(SEGUNDA_COLETA.toInstant());
    }

    @Test
    void deveVoltarAAtivaQuandoReativarOfertaDesativada() {
        Long id = ofertaRepository.saveAndFlush(ofertaDaSteam()).getId();
        em.clear();

        ofertaRepository.findById(id).orElseThrow().desativar(SEGUNDA_COLETA);
        em.flush();
        em.clear();

        OffsetDateTime terceiraColeta = SEGUNDA_COLETA.plusHours(6);
        ofertaRepository.findById(id).orElseThrow().reativar(terceiraColeta);
        em.flush();
        em.clear();

        OfertaEntity relida = ofertaRepository.findById(id).orElseThrow();
        assertThat(relida.isAtiva()).isTrue();
        assertThat(relida.getAtualizadoEm().toInstant()).isEqualTo(terceiraColeta.toInstant());
    }

    @Test
    void deveEncontrarOfertaDesativadaQuandoRecoletarMesmoIdentificador() {
        Long id = ofertaRepository.saveAndFlush(ofertaDaSteam()).getId();
        em.clear();

        ofertaRepository.findById(id).orElseThrow().desativar(SEGUNDA_COLETA);
        em.flush();
        em.clear();

        assertThat(ofertaRepository.findByLojaIdAndIdentificadorLoja(steam.getId(), IDENTIFICADOR))
                .isPresent()
                .get()
                .extracting(OfertaEntity::getId)
                .isEqualTo(id);
    }

}
