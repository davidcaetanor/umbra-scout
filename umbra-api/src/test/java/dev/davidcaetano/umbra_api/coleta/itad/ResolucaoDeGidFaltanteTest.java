package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.entity.OfertaEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaComUltimoPrecoProjecao;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.PrecoRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoRepository;
import dev.davidcaetano.umbra_api.coleta.Cobertura;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadDescobertaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;
import dev.davidcaetano.umbra_api.coleta.service.ColetaService;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResolucaoDeGidFaltanteTest extends IntegrationTestBase {

    private static final UUID GID_A = UUID.fromString("018d0000-0000-7000-8000-0000000000a1");
    private static final UUID GID_B = UUID.fromString("018d0000-0000-7000-8000-0000000000b2");

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private PrecoRepository precoRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private Clock clock;

    @Autowired
    private ColetaService coletaService;

    private ItadClientFalso itadClient;

    private ResolucaoDeGidFaltante resolucao;

    @BeforeEach
    void montarResolucaoComClientFalso() {
        itadClient = new ItadClientFalso();
        resolucao = new ResolucaoDeGidFaltante(itadClient, ofertaRepository, produtoRepository, transactionTemplate, clock);
    }

    private void semear(OfertaColetada... ofertas) {
        List<OfertaColetada> lista = List.of(ofertas);
        coletaService.gravar(new ResultadoColeta(lista.getFirst().origemColeta(), lista, lista.size(), 0,
                new Reconciliacao(null, lista.size(), lista.size()), Cobertura.amostra()));
    }

    private static OfertaColetada jogoDaSteamSemGid(String appid) {
        return new OfertaColetada(CodigoLoja.STEAM, appid, TipoProduto.JOGO, "Jogo " + appid, null,
                "https://store.steampowered.com/app/" + appid + "/", null, null,
                1000L, null, null, true, OrigemColeta.STEAM_API, null);
    }

    private static OfertaColetada jogoDoItad(CodigoLoja loja, String identificadorLoja, UUID gid) {
        return new OfertaColetada(loja, identificadorLoja, TipoProduto.JOGO, "Jogo " + identificadorLoja, null,
                "https://itad.link/" + identificadorLoja, null, gid.toString(),
                1000L, null, null, true, OrigemColeta.ITAD_API, null);
    }

    private static OfertaColetada pecaDaKabum(String identificadorLoja) {
        return new OfertaColetada(CodigoLoja.KABUM, identificadorLoja, TipoProduto.HARDWARE, "Peça " + identificadorLoja,
                "GPU", "https://www.kabum.com.br/produto/" + identificadorLoja, null, null,
                100000L, null, null, true, OrigemColeta.KABUM_API, null);
    }

    private ProdutoEntity produtoDaOferta(String identificadorLoja) {
        OfertaEntity oferta = ofertaRepository.findAll().stream()
                .filter(candidata -> candidata.getIdentificadorLoja().equals(identificadorLoja))
                .findFirst()
                .orElseThrow();
        return produtoRepository.findById(oferta.getProduto().getId()).orElseThrow();
    }

    @Test
    void resolver_deveCarimbarOGidDevolvidoNoProdutoDaSteamSemGid() {
        semear(jogoDaSteamSemGid("100"));
        itadClient.responder("app/100", GID_A);

        int carimbados = resolucao.resolver();

        assertThat(carimbados).isEqualTo(1);
        assertThat(produtoDaOferta("100").getChaveItad()).isEqualTo(GID_A.toString());
    }

    @Test
    void resolver_deveEnviarOIdentificadorComPrefixoAppParaALojaDaSteam() {
        semear(jogoDaSteamSemGid("100"));

        resolucao.resolver();

        assertThat(itadClient.shopIdsRecebidos).containsExactly(LojaItad.STEAM.shopId());
        assertThat(itadClient.identificadoresRecebidos).containsExactly("app/100");
    }

    @Test
    void resolver_naoDeveCarimbarGidQueJaPertenceAOutroProduto() {
        semear(jogoDoItad(CodigoLoja.NUUVEM, "jogo-dono", GID_A));
        semear(jogoDaSteamSemGid("100"));
        itadClient.responder("app/100", GID_A);

        int carimbados = resolucao.resolver();

        assertThat(carimbados).isZero();
        assertThat(produtoDaOferta("100").getChaveItad()).isNull();
        assertThat(produtoDaOferta("jogo-dono").getChaveItad()).isEqualTo(GID_A.toString());
    }

    @Test
    void resolver_deveCarimbarSoOPrimeiroQuandoDoisProdutosResolveremParaOMesmoGid() {
        semear(jogoDaSteamSemGid("100"));
        semear(jogoDaSteamSemGid("200"));
        itadClient.responder("app/100", GID_A);
        itadClient.responder("app/200", GID_A);

        int carimbados = resolucao.resolver();

        assertThat(carimbados).isEqualTo(1);
        assertThat(produtoDaOferta("100").getChaveItad()).isEqualTo(GID_A.toString());
        assertThat(produtoDaOferta("200").getChaveItad()).isNull();
    }

    @Test
    void resolver_naoDeveCarimbarQuandoOItadDevolverNulo() {
        semear(jogoDaSteamSemGid("100"));

        int carimbados = resolucao.resolver();

        assertThat(carimbados).isZero();
        assertThat(itadClient.identificadoresRecebidos).containsExactly("app/100");
        assertThat(produtoDaOferta("100").getChaveItad()).isNull();
    }

    @Test
    void resolver_naoDeveChamarOItadQuandoNaoHouverProdutoSemGid() {
        semear(jogoDoItad(CodigoLoja.STEAM, "100", GID_A));

        int carimbados = resolucao.resolver();

        assertThat(carimbados).isZero();
        assertThat(itadClient.shopIdsRecebidos).isEmpty();
    }

    @Test
    void resolver_naoDeveConsultarHardwareSemChaveNemJogoQueJaTemChave() {
        semear(pecaDaKabum("kabum-1"));
        semear(jogoDoItad(CodigoLoja.STEAM, "200", GID_B));
        semear(jogoDaSteamSemGid("100"));

        resolucao.resolver();

        assertThat(itadClient.identificadoresRecebidos).containsExactly("app/100");
    }

    @Test
    void resolver_deveTornarOProdutoVisivelAoRefreshDepoisDeCarimbado() {
        semear(jogoDaSteamSemGid("100"));
        itadClient.responder("app/100", GID_A);

        resolucao.resolver();

        List<OfertaComUltimoPrecoProjecao> conhecidas =
                precoRepository.findOfertasAtivasComUltimoPrecoPorCodigoLojaIn(List.of(CodigoLoja.STEAM.name()));
        assertThat(conhecidas).singleElement().satisfies(oferta -> {
            assertThat(oferta.getIdentificadorLoja()).isEqualTo("100");
            assertThat(oferta.getChaveItad()).isEqualTo(GID_A.toString());
        });
    }

    private static final class ItadClientFalso implements ItadClient {

        private final Map<String, UUID> gidPorIdentificador = new HashMap<>();
        private final List<Integer> shopIdsRecebidos = new ArrayList<>();
        private final List<String> identificadoresRecebidos = new ArrayList<>();

        void responder(String identificadorItad, UUID gid) {
            gidPorIdentificador.put(identificadorItad, gid);
        }

        @Override
        public Map<String, UUID> resolverGidPorIdentificador(int shopId, List<String> identificadoresItad) {
            shopIdsRecebidos.add(shopId);
            identificadoresRecebidos.addAll(identificadoresItad);

            Map<String, UUID> resposta = new HashMap<>();
            identificadoresItad.forEach(identificador -> resposta.put(identificador, gidPorIdentificador.get(identificador)));
            return resposta;
        }

        @Override
        public ItadDescobertaResponse buscarDescoberta(int offset) {
            throw new UnsupportedOperationException("a resolucao de gid nao usa a descoberta");
        }

        @Override
        public Map<UUID, List<String>> resolverIdentificadorNativo(int shopId, List<UUID> gids) {
            throw new UnsupportedOperationException("a resolucao de gid nao usa o lookup direto");
        }

        @Override
        public List<ItadPrecoJogoResponse> buscarPrecos(List<UUID> gids) {
            throw new UnsupportedOperationException("a resolucao de gid nao usa precos");
        }
    }
}
