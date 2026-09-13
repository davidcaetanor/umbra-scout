package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.LojaEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.PrecoEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity.DadosProduto;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class PrecoRepositoryTest extends IntegrationTestBase {

    private static final OffsetDateTime PRIMEIRA_COLETA = OffsetDateTime.parse("2026-09-01T09:00:00Z");
    private static final OffsetDateTime SEGUNDA_COLETA = OffsetDateTime.parse("2026-09-01T15:00:00Z");

    @Autowired
    private PrecoRepository precoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private LojaRepository lojaRepository;

    @Autowired
    private EntityManager em;

    private ProdutoEntity produto;

    @BeforeEach
    void criarProduto() {
        LojaEntity steam = lojaRepository.findByCodigo(CodigoLoja.STEAM).orElseThrow();
        produto = produtoRepository.saveAndFlush(ProdutoEntity.novo(
                steam, "app-730", TipoProduto.JOGO,
                new DadosProduto("Counter-Strike 2", "Acao",
                        "https://store.steampowered.com/app/730", null, null),
                PRIMEIRA_COLETA));
    }

    @Test
    void deveGerarDuasLinhasQuandoColetarOMesmoProdutoDuasVezes() {
        precoRepository.save(PrecoEntity.novo(produto, 8990L, 12990L, (short) 30, true,
                OrigemColeta.STEAM_API, null, PRIMEIRA_COLETA));
        precoRepository.save(PrecoEntity.novo(produto, 7490L, 12990L, (short) 42, true,
                OrigemColeta.STEAM_API, null, SEGUNDA_COLETA));
        em.flush();

        assertThat(precoRepository.count()).isEqualTo(2);
    }

    @Test
    void deveDesempatarPrecoAtualPorIdMaisRecenteEmColetasSimultaneas() {
        OffsetDateTime mesmoInstante = OffsetDateTime.parse("2026-09-01T12:00:00Z");

        PrecoEntity primeiro = precoRepository.saveAndFlush(PrecoEntity.novo(produto, 9000L, null, null,
                true, OrigemColeta.STEAM_API, null, mesmoInstante));
        PrecoEntity segundo = precoRepository.saveAndFlush(PrecoEntity.novo(produto, 8000L, null, null,
                true, OrigemColeta.STEAM_API, null, mesmoInstante));

        assertThat(segundo.getId()).isGreaterThan(primeiro.getId());

        Object precoIdDaView = em.createNativeQuery(
                        "SELECT preco_id FROM vw_preco_atual WHERE produto_id = :produtoId")
                .setParameter("produtoId", produto.getId())
                .getSingleResult();

        assertThat(((Number) precoIdDaView).longValue()).isEqualTo(segundo.getId());
    }

    @Test
    void deveDevolverColetaMaisRecenteQuandoConsultarPrecoAtual() {
        precoRepository.saveAndFlush(PrecoEntity.novo(produto, 9000L, null, null, true,
                OrigemColeta.STEAM_API, null, PRIMEIRA_COLETA));
        PrecoEntity maisRecente = precoRepository.saveAndFlush(PrecoEntity.novo(produto, 7490L, 12990L,
                (short) 42, true, OrigemColeta.STEAM_API, null, SEGUNDA_COLETA));

        Object valorNaView = em.createNativeQuery(
                        "SELECT valor_centavos FROM vw_preco_atual WHERE produto_id = :produtoId")
                .setParameter("produtoId", produto.getId())
                .getSingleResult();

        assertThat(((Number) valorNaView).longValue()).isEqualTo(maisRecente.getValorCentavos());
    }

    @Test
    void deveGravarOrigemColetaComoTextoQuandoPersistirPreco() {
        Long id = precoRepository.saveAndFlush(PrecoEntity.novo(produto, 8990L, null, null, true,
                OrigemColeta.STEAM_API, null, PRIMEIRA_COLETA)).getId();

        Object origemNaColuna = em.createNativeQuery("SELECT origem_coleta FROM preco WHERE id = :id")
                .setParameter("id", id)
                .getSingleResult();

        assertThat(origemNaColuna).isEqualTo("STEAM_API");
    }

    @Test
    void devePreservarDadosDaColetaQuandoPersistirPreco() {
        Long id = precoRepository.saveAndFlush(PrecoEntity.novo(produto, 8990L, 12990L, (short) 30, true,
                OrigemColeta.STEAM_API, null, PRIMEIRA_COLETA)).getId();
        em.clear();

        PrecoEntity relido = precoRepository.findById(id).orElseThrow();

        assertThat(relido.getColetadoEm().toInstant()).isEqualTo(PRIMEIRA_COLETA.toInstant());
        assertThat(relido.getValorCentavos()).isEqualTo(8990L);
        assertThat(relido.getValorOriginalCentavos()).isEqualTo(12990L);
        assertThat(relido.getDescontoPct()).isEqualTo((short) 30);
        assertThat(relido.getProduto().getId()).isEqualTo(produto.getId());
    }

    @Test
    void deveGravarPrecoIndisponivelQuandoItemEstiverEsgotado() {
        Long id = precoRepository.saveAndFlush(PrecoEntity.novo(produto, 8990L, null, null, false,
                OrigemColeta.STEAM_API, null, PRIMEIRA_COLETA)).getId();
        em.clear();

        assertThat(precoRepository.findById(id).orElseThrow().isDisponivel()).isFalse();
    }

    @Test
    void deveGravarSemDescontoQuandoColetaNaoTrouxerValorOriginal() {
        Long id = precoRepository.saveAndFlush(PrecoEntity.novo(produto, 8990L, null, null, true,
                OrigemColeta.STEAM_API, null, PRIMEIRA_COLETA)).getId();
        em.clear();

        PrecoEntity relido = precoRepository.findById(id).orElseThrow();

        assertThat(relido.getValorOriginalCentavos()).isNull();
        assertThat(relido.getDescontoPct()).isNull();
    }

    @Test
    void devePreservarInstanteDeExpiraEmQuandoPersistirComOffsetNaoUtc() {
        OffsetDateTime expiraEm = OffsetDateTime.parse("2026-09-05T09:00:00-03:00");

        Long id = precoRepository.saveAndFlush(PrecoEntity.novo(produto, 8990L, null, null, true,
                OrigemColeta.STEAM_API, expiraEm, PRIMEIRA_COLETA)).getId();
        em.clear();

        PrecoEntity relido = precoRepository.findById(id).orElseThrow();

        assertThat(relido.getExpiraEm().toInstant()).isEqualTo(expiraEm.toInstant());
    }
}
