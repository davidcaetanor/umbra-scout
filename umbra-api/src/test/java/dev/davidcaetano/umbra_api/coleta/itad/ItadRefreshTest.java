package dev.davidcaetano.umbra_api.coleta.itad;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.davidcaetano.umbra_api.catalogo.entity.OfertaEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.PrecoEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.PrecoRepository;
import dev.davidcaetano.umbra_api.coleta.Cobertura;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.service.ColetaService;
import dev.davidcaetano.umbra_api.coleta.service.SituacaoRodada;
import dev.davidcaetano.umbra_api.coleta.service.VereditoRodada;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class ItadRefreshTest extends IntegrationTestBase {

    private static final String GID_A = "018d0000-0000-7000-8000-0000000000a1";
    private static final String GID_B = "018d0000-0000-7000-8000-0000000000b2";
    private static final String GID_NAO_PEDIDO = "018d0000-0000-7000-8000-0000000000c3";
    private static final String PRECOS_V3 = "/games/prices/v3";
    private static final String EXPIRY_NULO = "null";

    @RegisterExtension
    static WireMockExtension WIREMOCK = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true))
            .build();

    @DynamicPropertySource
    static void configurarBaseUrlItad(DynamicPropertyRegistry registry) {
        registry.add("itad.itad-api.base-url", WIREMOCK::baseUrl);
    }

    @Autowired
    private ItadRefresh itadRefresh;

    @Autowired
    private ColetaService coletaService;

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private PrecoRepository precoRepository;

    private static OfertaColetada jogoDoItad(CodigoLoja loja, String identificadorLoja, String chaveItad,
                                             long valorCentavos, String url) {
        return new OfertaColetada(loja, identificadorLoja, TipoProduto.JOGO, "Jogo " + identificadorLoja, null,
                url, null, chaveItad, valorCentavos, null, null, true, OrigemColeta.ITAD_API, null);
    }

    private static OfertaColetada jogoDaSteamSemChave(String identificadorLoja, long valorCentavos) {
        return new OfertaColetada(CodigoLoja.STEAM, identificadorLoja, TipoProduto.JOGO, "Jogo " + identificadorLoja,
                null, "https://store.steampowered.com/app/" + identificadorLoja + "/", null, null,
                valorCentavos, null, null, true, OrigemColeta.STEAM_API, null);
    }

    private void semear(OfertaColetada... ofertas) {
        List<OfertaColetada> lista = List.of(ofertas);
        coletaService.gravar(new ResultadoColeta(lista.getFirst().origemColeta(), lista, lista.size(), 0,
                new Reconciliacao(null, lista.size(), lista.size()), Cobertura.amostra()));
    }

    private static String dealJson(int shopId, int precoAtual, String moedaPreco, int precoOriginal, int desconto,
                                   String dealUrl) {
        return """
                {
                  "shop": { "id": %d, "name": "Loja %d" },
                  "price": { "amountInt": %d, "currency": "%s" },
                  "regular": { "amountInt": %d, "currency": "%s" },
                  "cut": %d,
                  "timestamp": "2026-09-01T00:00:00Z",
                  "expiry": %s,
                  "url": "%s"
                }
                """.formatted(shopId, shopId, precoAtual, moedaPreco, precoOriginal, moedaPreco, desconto,
                EXPIRY_NULO, dealUrl);
    }

    private static String dealBrl(int shopId, int precoAtual, int precoOriginal, int desconto, String dealUrl) {
        return dealJson(shopId, precoAtual, "BRL", precoOriginal, desconto, dealUrl);
    }

    private static String precoJogoJson(String gid, String... deals) {
        return """
                { "id": "%s", "deals": [%s] }
                """.formatted(gid, String.join(",", deals));
    }

    private static String precosResponse(String... jogos) {
        return "[" + String.join(",", jogos) + "]";
    }

    private static void stubPrecos(String corpo) {
        WIREMOCK.stubFor(post(urlPathEqualTo(PRECOS_V3))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(corpo)));
    }

    private OfertaEntity ofertaPorIdentificador(String identificadorLoja) {
        return ofertaRepository.findAll().stream()
                .filter(oferta -> oferta.getIdentificadorLoja().equals(identificadorLoja))
                .findFirst()
                .orElseThrow();
    }

    private List<PrecoEntity> precosDa(String identificadorLoja) {
        Long ofertaId = ofertaPorIdentificador(identificadorLoja).getId();
        return precoRepository.findAll().stream()
                .filter(preco -> preco.getOferta().getId().equals(ofertaId))
                .sorted(Comparator.comparing(PrecoEntity::getColetadoEm).thenComparing(PrecoEntity::getId))
                .toList();
    }

    private static Map<String, Long> valorPorIdentificador(ResultadoColeta resultado) {
        return resultado.ofertas().stream()
                .collect(Collectors.toMap(OfertaColetada::identificadorLoja, OfertaColetada::valorCentavos));
    }

    @Test
    void atualizar_deveUsarOIdentificadorDaOfertaDoBancoSemChamarLookup() {
        semear(jogoDoItad(CodigoLoja.STEAM, "100", GID_A, 1000L, "https://itad.link/a"));

        stubPrecos(precosResponse(precoJogoJson(GID_A, dealBrl(61, 1000, 5000, 80, "https://itad.link/a"))));

        ResultadoColeta resultado = itadRefresh.atualizar();

        WIREMOCK.verify(0, postRequestedFor(urlPathMatching("/lookup/.*")));
        assertThat(resultado.ofertas()).singleElement().satisfies(oferta -> {
            assertThat(oferta.loja()).isEqualTo(CodigoLoja.STEAM);
            assertThat(oferta.identificadorLoja()).isEqualTo("100");
            assertThat(oferta.nome()).isEqualTo("Jogo 100");
            assertThat(oferta.chaveItad()).isEqualTo(GID_A);
            assertThat(oferta.valorOriginalCentavos()).isEqualTo(5000L);
            assertThat(oferta.descontoPct()).isEqualTo((short) 80);
            assertThat(oferta.categoria()).isNull();
            assertThat(oferta.imagemUrl()).isNull();
            assertThat(oferta.disponivel()).isTrue();
            assertThat(oferta.origemColeta()).isEqualTo(OrigemColeta.ITAD_API);
        });
    }

    @Test
    void atualizar_deveFazerDuasChamadasDePrecoQuandoOCatalogoTiver201Jogos() {
        List<OfertaColetada> jogos = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            jogos.add(jogoDoItad(CodigoLoja.STEAM, "jogo-" + i,
                    "018d0000-0000-7000-8000-%012d".formatted(i), 1000L, "https://itad.link/" + i));
        }
        semear(jogos.toArray(OfertaColetada[]::new));

        stubPrecos(precosResponse());

        itadRefresh.atualizar();

        WIREMOCK.verify(2, postRequestedFor(urlPathEqualTo(PRECOS_V3)));
    }

    @Test
    void atualizar_deveCasarPorIdQuandoARespostaVierEmOrdemDiferenteDaRequisicao() {
        semear(
                jogoDoItad(CodigoLoja.STEAM, "100", GID_A, 1000L, "https://itad.link/a"),
                jogoDoItad(CodigoLoja.STEAM, "200", GID_B, 2000L, "https://itad.link/b"));

        stubPrecos(precosResponse(
                precoJogoJson(GID_B, dealBrl(61, 2200, 4000, 45, "https://itad.link/b")),
                precoJogoJson(GID_A, dealBrl(61, 1100, 4000, 72, "https://itad.link/a"))));

        ResultadoColeta resultado = itadRefresh.atualizar();

        assertThat(valorPorIdentificador(resultado)).containsExactlyInAnyOrderEntriesOf(Map.of("100", 1100L, "200", 2200L));
    }

    @Test
    void atualizar_deveFicarComOMenorPrecoQuandoHouverDuasOfertasNoMesmoPar() {
        semear(jogoDoItad(CodigoLoja.NUUVEM, "jogo-a", GID_A, 3000L, "https://itad.link/a-cara"));

        stubPrecos(precosResponse(precoJogoJson(GID_A,
                dealBrl(50, 3000, 6000, 50, "https://itad.link/a-cara"),
                dealBrl(50, 2500, 6000, 58, "https://itad.link/a-barata"))));

        ResultadoColeta resultado = itadRefresh.atualizar();

        assertThat(resultado.ofertas()).singleElement().satisfies(oferta -> {
            assertThat(oferta.valorCentavos()).isEqualTo(2500L);
            assertThat(oferta.url()).isEqualTo("https://itad.link/a-barata");
        });
        assertThat(resultado.totalElegivel()).isEqualTo(1);
    }

    @Test
    void atualizar_deveColetarJogoGratuitoComValorZeroEDisponivel() {
        semear(jogoDoItad(CodigoLoja.STEAM, "730", GID_A, 0L, "https://itad.link/cs2"));

        stubPrecos(precosResponse(precoJogoJson(GID_A, dealBrl(61, 0, 0, 0, "https://itad.link/cs2"))));

        ResultadoColeta resultado = itadRefresh.atualizar();

        assertThat(resultado.ofertas()).singleElement().satisfies(oferta -> {
            assertThat(oferta.valorCentavos()).isZero();
            assertThat(oferta.valorOriginalCentavos()).isZero();
            assertThat(oferta.disponivel()).isTrue();
        });
        assertThat(resultado.totalSemPreco()).isZero();
    }

    @Test
    void atualizar_naoDeveMontarOfertaParaParSemOfertaConhecida() {
        semear(jogoDoItad(CodigoLoja.STEAM, "100", GID_A, 1000L, "https://itad.link/a"));

        stubPrecos(precosResponse(precoJogoJson(GID_A,
                dealBrl(61, 1000, 5000, 80, "https://itad.link/a"),
                dealBrl(50, 900, 5000, 82, "https://itad.link/a-nuuvem"))));

        ResultadoColeta resultado = itadRefresh.atualizar();

        assertThat(resultado.ofertas()).singleElement()
                .satisfies(oferta -> assertThat(oferta.loja()).isEqualTo(CodigoLoja.STEAM));
        assertThat(resultado.totalElegivel()).isEqualTo(1);
    }

    @Test
    void atualizar_naoDeveMontarNemContarComoElegivelOfertaForaDeBrl() {
        semear(jogoDoItad(CodigoLoja.STEAM, "100", GID_A, 1000L, "https://itad.link/a"));

        stubPrecos(precosResponse(precoJogoJson(GID_A,
                dealJson(61, 1000, "USD", 5000, 80, "https://itad.link/a"))));

        ResultadoColeta resultado = itadRefresh.atualizar();

        assertThat(resultado.ofertas()).isEmpty();
        assertThat(resultado.totalElegivel()).isZero();
        assertThat(resultado.totalSemPreco()).isZero();
    }

    @Test
    void atualizar_deveDeclararCensoDosGidsPedidosNasLojasDoItad() {
        semear(
                jogoDoItad(CodigoLoja.STEAM, "100", GID_A, 1000L, "https://itad.link/a"),
                jogoDoItad(CodigoLoja.GOG, "jogo-b", GID_B, 2000L, "https://itad.link/b"));

        stubPrecos(precosResponse());

        Cobertura cobertura = itadRefresh.atualizar().cobertura();

        assertThat(cobertura.ehCenso()).isTrue();
        assertThat(cobertura.lojasEmCenso()).containsExactlyInAnyOrderElementsOf(LojaItad.codigosLoja());
        assertThat(cobertura.cobre(CodigoLoja.STEAM, GID_A)).isTrue();
        assertThat(cobertura.cobre(CodigoLoja.NUUVEM, GID_B)).isTrue();
        assertThat(cobertura.cobre(CodigoLoja.STEAM, GID_NAO_PEDIDO)).isFalse();
        assertThat(cobertura.cobre(CodigoLoja.STEAM, null)).isFalse();
    }

    @Test
    void atualizar_naoDeveChamarOItadQuandoNenhumJogoDoCatalogoTiverChave() {
        semear(jogoDaSteamSemChave("300", 2000L));

        ResultadoColeta resultado = itadRefresh.atualizar();

        WIREMOCK.verify(0, postRequestedFor(urlPathEqualTo(PRECOS_V3)));
        assertThat(resultado.ofertas()).isEmpty();
        assertThat(resultado.cobertura().ehCenso()).isFalse();
    }

    @Test
    void atualizarEGravar_deveAtualizarPrecoMarcarAusenteManterSemChaveETrocarUrl() {
        semear(
                jogoDoItad(CodigoLoja.STEAM, "100", GID_A, 1000L, "https://itad.link/a-antiga"),
                jogoDoItad(CodigoLoja.STEAM, "200", GID_B, 3000L, "https://itad.link/b"));
        semear(jogoDaSteamSemChave("300", 2000L));

        stubPrecos(precosResponse(precoJogoJson(GID_A, dealBrl(61, 5000, 5000, 0, "https://itad.link/a-nova"))));

        VereditoRodada veredito = coletaService.gravar(itadRefresh.atualizar());

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);

        List<PrecoEntity> precosDaQueSaiuDePromocao = precosDa("100");
        assertThat(precosDaQueSaiuDePromocao).extracting(PrecoEntity::getValorCentavos).containsExactly(1000L, 5000L);
        assertThat(precosDaQueSaiuDePromocao).allSatisfy(preco -> assertThat(preco.isDisponivel()).isTrue());

        List<PrecoEntity> precosDaQueNaoVoltou = precosDa("200");
        assertThat(precosDaQueNaoVoltou).hasSize(2);
        assertThat(precosDaQueNaoVoltou.getLast().isDisponivel()).isFalse();
        assertThat(precosDaQueNaoVoltou.getLast().getValorCentavos()).isEqualTo(3000L);

        assertThat(precosDa("300")).hasSize(1);
        assertThat(ofertaPorIdentificador("100").getUrl()).isEqualTo("https://itad.link/a-nova");
    }

    @Test
    void atualizarEGravar_deveRejeitarENaoMarcarAusenciaQuandoTodosOsLotesVieremVazios() {
        semear(
                jogoDoItad(CodigoLoja.STEAM, "100", GID_A, 1000L, "https://itad.link/a"),
                jogoDoItad(CodigoLoja.STEAM, "200", GID_B, 3000L, "https://itad.link/b"));

        stubPrecos(precosResponse());

        VereditoRodada veredito = coletaService.gravar(itadRefresh.atualizar());

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.REJEITADA);
        assertThat(precosDa("100")).hasSize(1);
        assertThat(precosDa("200")).hasSize(1);
        assertThat(precoRepository.findAll()).allSatisfy(preco -> assertThat(preco.isDisponivel()).isTrue());
    }
}
