package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.catalogo.entity.OfertaEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.PrecoEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.catalogo.repository.LojaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.PrecoRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoRepository;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@ActiveProfiles("test")
@Import(ColetaServiceTest.RelogioDeTesteConfig.class)
@Transactional
class ColetaServiceTest extends IntegrationTestBase {

    private static final OffsetDateTime INSTANTE_RODADA_1 = OffsetDateTime.parse("2026-06-01T09:00:00-03:00");

    @Autowired
    private ColetaService coletaService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private PrecoRepository precoRepository;

    @Autowired
    private LojaRepository lojaRepository;

    @Autowired
    private Clock clock;

    private RelogioMutavel relogio;

    @BeforeEach
    void reiniciarRelogio() {
        relogio = (RelogioMutavel) clock;
        relogio.avancarPara(INSTANTE_RODADA_1.toInstant());
    }

    private void avancar(java.time.Duration duracao) {
        relogio.avancarPara(relogio.instant().plus(duracao));
    }

    private OfertaColetada oferta(CodigoLoja loja, String identificadorLoja, TipoProduto tipo, String nome,
                                   String chaveItad, long valorCentavos, boolean disponivel, OrigemColeta origemColeta) {
        return oferta(loja, identificadorLoja, tipo, nome, chaveItad, valorCentavos, disponivel, origemColeta, null);
    }

    private OfertaColetada oferta(CodigoLoja loja, String identificadorLoja, TipoProduto tipo, String nome,
                                   String chaveItad, long valorCentavos, boolean disponivel, OrigemColeta origemColeta,
                                   OffsetDateTime expiry) {
        return new OfertaColetada(loja, identificadorLoja, tipo, nome, "Acao",
                "https://loja.exemplo/" + identificadorLoja, null, chaveItad,
                valorCentavos, null, null, disponivel, origemColeta, expiry);
    }

    private VereditoRodada gravar(OfertaColetada... ofertas) {
        List<OfertaColetada> lista = List.of(ofertas);
        return coletaService.gravar(new ResultadoColeta(ofertas[0].origemColeta(), lista, lista.size(), 0,
                new Reconciliacao(null, lista.size(), lista.size())));
    }

    @Test
    void deveCriarProdutoOfertaEPrecoQuandoOfertaForNova() {
        OffsetDateTime expiry = INSTANTE_RODADA_1.plusDays(5);

        gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, true, OrigemColeta.STEAM_API, expiry));

        assertThat(produtoRepository.count()).isEqualTo(1);
        assertThat(ofertaRepository.count()).isEqualTo(1);
        assertThat(precoRepository.count()).isEqualTo(1);

        PrecoEntity preco = precoRepository.findAll().getFirst();
        assertThat(preco.getExpiraEm().toInstant()).isEqualTo(expiry.toInstant());
    }

    @Test
    void deveCriarUmProdutoUnicoQuandoDuasOfertasCompartilhamAChaveItad() {
        gravar(
                oferta(CodigoLoja.STEAM, "1245620", TipoProduto.JOGO, "Elden Ring",
                        "gid-elden-ring", 19990L, true, OrigemColeta.STEAM_API),
                oferta(CodigoLoja.NUUVEM, "elden-ring", TipoProduto.JOGO, "Elden Ring",
                        "gid-elden-ring", 17990L, true, OrigemColeta.ITAD_API));

        assertThat(produtoRepository.count()).isEqualTo(1);
        assertThat(ofertaRepository.count()).isEqualTo(2);
        assertThat(precoRepository.count()).isEqualTo(2);
    }

    @Test
    void deveReaproveitarProdutoECarimbarChaveItadQuandoOfertaJaExistirSemEla() {
        gravar(oferta(CodigoLoja.STEAM, "440", TipoProduto.JOGO, "Team Fortress 2",
                null, 0L, true, OrigemColeta.STEAM_API));

        assertThat(produtoRepository.count()).isEqualTo(1);
        Long idDoProduto = produtoRepository.findAll().getFirst().getId();

        avancar(java.time.Duration.ofHours(6));

        gravar(
                oferta(CodigoLoja.STEAM, "440", TipoProduto.JOGO, "Team Fortress 2",
                        "gid-tf2", 0L, true, OrigemColeta.STEAM_API),
                oferta(CodigoLoja.GOG, "tf2-gog", TipoProduto.JOGO, "Team Fortress 2",
                        "gid-tf2", 0L, true, OrigemColeta.ITAD_API));

        assertThat(produtoRepository.count()).isEqualTo(1);

        ProdutoEntity produto = produtoRepository.findById(idDoProduto).orElseThrow();
        assertThat(produto.getChaveItad()).isEqualTo("gid-tf2");

        List<OfertaEntity> ofertas = ofertaRepository.findAll();
        assertThat(ofertas).hasSize(2);
        assertThat(ofertas).allSatisfy(o -> assertThat(o.getProduto().getId()).isEqualTo(idDoProduto));
    }

    @Test
    void naoDeveGravarPrecoQuandoValorEDisponibilidadeNaoMudarem() {
        gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, true, OrigemColeta.STEAM_API));

        avancar(java.time.Duration.ofHours(6));

        gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, true, OrigemColeta.STEAM_API));

        assertThat(precoRepository.count()).isEqualTo(1);
    }

    @Test
    void deveGravarPrecoQuandoValorMudar() {
        gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, true, OrigemColeta.STEAM_API));

        avancar(java.time.Duration.ofHours(6));

        gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 7990L, true, OrigemColeta.STEAM_API));

        assertThat(precoRepository.count()).isEqualTo(2);
    }

    @Test
    void deveGravarPrecoQuandoApenasDisponibilidadeMudar() {
        gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, true, OrigemColeta.STEAM_API));

        avancar(java.time.Duration.ofHours(6));

        gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, false, OrigemColeta.STEAM_API));

        assertThat(precoRepository.count()).isEqualTo(2);
    }

    @Test
    void deveGravarPrecoQuandoUltimoRegistroPassarDaJanelaDeVivacidade() {
        gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, true, OrigemColeta.STEAM_API));

        avancar(java.time.Duration.ofDays(91));

        gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, true, OrigemColeta.STEAM_API));

        assertThat(precoRepository.count()).isEqualTo(2);
    }

    @Test
    void deveCarimbarOMesmoInstanteEmTodasAsLinhasDaRodada() {
        gravar(
                oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                        "gid-cs2", 8990L, true, OrigemColeta.STEAM_API),
                oferta(CodigoLoja.NUUVEM, "elden-ring", TipoProduto.JOGO, "Elden Ring",
                        "gid-elden-ring", 17990L, true, OrigemColeta.ITAD_API),
                oferta(CodigoLoja.KABUM, "123456", TipoProduto.HARDWARE, "Placa de Vídeo RTX 4070",
                        null, 349900L, true, OrigemColeta.KABUM_API));

        List<PrecoEntity> precos = precoRepository.findAll();
        assertThat(precos).hasSize(3);
        assertThat(precos).allSatisfy(preco ->
                assertThat(preco.getColetadoEm().toInstant()).isEqualTo(INSTANTE_RODADA_1.toInstant()));
    }

    @Test
    void deveCriarProdutoUnicoPorOfertaQuandoHardwareNaoTiverChaveItad() {
        gravar(
                oferta(CodigoLoja.KABUM, "111", TipoProduto.HARDWARE, "Placa de Vídeo RTX 4070",
                        null, 349900L, true, OrigemColeta.KABUM_API),
                oferta(CodigoLoja.KABUM, "222", TipoProduto.HARDWARE, "Processador Ryzen 7",
                        null, 189900L, true, OrigemColeta.KABUM_API));

        assertThat(produtoRepository.count()).isEqualTo(2);
        assertThat(ofertaRepository.count()).isEqualTo(2);
    }

    @Test
    void deveManterProdutoEOfertaAtivosAposGravar() {
        gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, true, OrigemColeta.STEAM_API));

        assertThat(produtoRepository.findAll()).allSatisfy(produto -> assertThat(produto.isAtivo()).isTrue());
        assertThat(ofertaRepository.findAll()).allSatisfy(oferta -> assertThat(oferta.isAtiva()).isTrue());
    }

    @Test
    void deveFalharComMensagemClaraQuandoLojaDaOfertaNaoEstiverSemeada() {
        lojaRepository.delete(lojaRepository.findByCodigo(CodigoLoja.GREEN_MAN_GAMING).orElseThrow());

        Throwable falha = catchThrowable(() -> gravar(oferta(CodigoLoja.GREEN_MAN_GAMING, "123",
                TipoProduto.JOGO, "Jogo Sem Loja Semeada", null, 1000L, true, OrigemColeta.ITAD_API)));

        assertThat(falha).isInstanceOf(IllegalStateException.class);
        assertThat(falha.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GREEN_MAN_GAMING");
    }

    @Test
    void naoDeveGravarNadaQuandoRodadaForRejeitada() {
        List<OfertaColetada> ofertas = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            ofertas.add(oferta(CodigoLoja.KABUM, "peca-" + i, TipoProduto.HARDWARE, "Peça " + i,
                    null, 10000L, true, OrigemColeta.KABUM_API));
        }

        VereditoRodada veredito = coletaService.gravar(new ResultadoColeta(OrigemColeta.KABUM_API, ofertas, 10, 3,
                new Reconciliacao(null, 10, 10)));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.REJEITADA);
        assertThat(produtoRepository.count()).isZero();
        assertThat(ofertaRepository.count()).isZero();
        assertThat(precoRepository.count()).isZero();
    }

    @Test
    void deveGravarOQueVeioQuandoRodadaForDegradada() {
        List<OfertaColetada> ofertas = List.of(
                oferta(CodigoLoja.KABUM, "111", TipoProduto.HARDWARE, "Placa de Vídeo RTX 4070",
                        null, 349900L, true, OrigemColeta.KABUM_API),
                oferta(CodigoLoja.KABUM, "222", TipoProduto.HARDWARE, "Processador Ryzen 7",
                        null, 189900L, true, OrigemColeta.KABUM_API));

        VereditoRodada veredito = coletaService.gravar(new ResultadoColeta(OrigemColeta.KABUM_API, ofertas, 2, 0,
                new Reconciliacao(10L, 2, 2)));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.DEGRADADA);
        assertThat(ofertaRepository.count()).isEqualTo(2);
        assertThat(precoRepository.count()).isEqualTo(2);
    }

    @Test
    void deveDevolverSaudavelQuandoRodadaNaoDispararNenhumaCondicao() {
        VereditoRodada veredito = gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, true, OrigemColeta.STEAM_API));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @TestConfiguration
    static class RelogioDeTesteConfig {

        @Bean
        @Primary
        Clock relogioMutavelDeTeste() {
            return new RelogioMutavel(INSTANTE_RODADA_1.toInstant());
        }
    }

    private static final class RelogioMutavel extends Clock {

        private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

        private Instant instante;

        private RelogioMutavel(Instant instanteInicial) {
            this.instante = instanteInicial;
        }

        void avancarPara(Instant novoInstante) {
            this.instante = novoInstante;
        }

        @Override
        public ZoneId getZone() {
            return ZONA;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException("relógio de teste tem fuso fixo");
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
