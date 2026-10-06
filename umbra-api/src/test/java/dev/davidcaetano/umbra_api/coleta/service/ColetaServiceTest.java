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
import dev.davidcaetano.umbra_api.coleta.Cobertura;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
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

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

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
                new Reconciliacao(null, lista.size(), lista.size()), Cobertura.amostra()));
    }

    private VereditoRodada gravarCenso(CodigoLoja loja, Reconciliacao reconciliacao, int totalSemPreco,
                                       List<OfertaColetada> ofertas) {
        return coletaService.gravar(new ResultadoColeta(ofertas.getFirst().origemColeta(), ofertas,
                ofertas.size() + totalSemPreco, totalSemPreco, reconciliacao, Cobertura.censoDe(loja)));
    }

    private VereditoRodada gravarCensoLimpoDaKabum(OfertaColetada... ofertas) {
        return gravarCenso(CodigoLoja.KABUM, new Reconciliacao((long) ofertas.length, ofertas.length, ofertas.length),
                0, List.of(ofertas));
    }

    private OfertaColetada pecaKabum(String identificadorLoja, long valorCentavos, boolean disponivel) {
        return oferta(CodigoLoja.KABUM, identificadorLoja, TipoProduto.HARDWARE, "Peça " + identificadorLoja,
                null, valorCentavos, disponivel, OrigemColeta.KABUM_API);
    }

    private List<OfertaColetada> pecasKabum(int quantidade) {
        List<OfertaColetada> pecas = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            pecas.add(pecaKabum("peca-" + i, 10000L, true));
        }
        return pecas;
    }

    private List<PrecoEntity> precosDa(String identificadorLoja) {
        Long ofertaId = ofertaPorIdentificador(identificadorLoja).getId();
        return precoRepository.findAll().stream()
                .filter(preco -> preco.getOferta().getId().equals(ofertaId))
                .sorted(Comparator.comparing(PrecoEntity::getColetadoEm).thenComparing(PrecoEntity::getId))
                .toList();
    }

    private List<Long> ofertasNaMelhorOfertaAtual() {
        entityManager.flush();
        return jdbc.queryForList("SELECT oferta_id FROM vw_melhor_oferta_atual", Long.class);
    }

    private OfertaEntity ofertaPorIdentificador(String identificadorLoja) {
        return ofertaRepository.findAll().stream()
                .filter(oferta -> oferta.getIdentificadorLoja().equals(identificadorLoja))
                .findFirst()
                .orElseThrow();
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
                new Reconciliacao(null, 10, 10), Cobertura.amostra()));

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
                new Reconciliacao(10L, 2, 2), Cobertura.amostra()));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.DEGRADADA);
        assertThat(ofertaRepository.count()).isEqualTo(2);
        assertThat(precoRepository.count()).isEqualTo(2);
    }

    @Test
    void deveGravarOfertaGratuitaEElegerComoMelhorOfertaDoProduto() {
        gravar(
                oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                        "gid-cs2", 0L, true, OrigemColeta.ITAD_API),
                oferta(CodigoLoja.NUUVEM, "cs2-nuuvem", TipoProduto.JOGO, "Counter-Strike 2",
                        "gid-cs2", 4990L, true, OrigemColeta.ITAD_API));

        entityManager.flush();

        assertThat(precoRepository.count()).isEqualTo(2);

        OfertaEntity gratuita = ofertaPorIdentificador("730");
        Long melhorOferta = jdbc.queryForObject(
                "SELECT oferta_id FROM vw_melhor_oferta_atual WHERE produto_id = ?",
                Long.class, gratuita.getProduto().getId());

        assertThat(melhorOferta).isEqualTo(gratuita.getId());
    }

    @Test
    void deveDevolverSaudavelQuandoRodadaNaoDispararNenhumaCondicao() {
        VereditoRodada veredito = gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, true, OrigemColeta.STEAM_API));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
    }

    @Test
    void ausencia_deveMarcarIndisponivelComUltimoValorQuandoOfertaNaoVoltarEmCensoLimpo() {
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, true));

        avancar(Duration.ofHours(6));
        OffsetDateTime instanteRodada2 = OffsetDateTime.now(clock);

        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true));

        List<PrecoEntity> precosDaAusente = precosDa("222");
        assertThat(precosDaAusente).hasSize(2);

        PrecoEntity ausencia = precosDaAusente.getLast();
        assertThat(ausencia.isDisponivel()).isFalse();
        assertThat(ausencia.getValorCentavos()).isEqualTo(20000L);
        assertThat(ausencia.getValorOriginalCentavos()).isNull();
        assertThat(ausencia.getDescontoPct()).isNull();
        assertThat(ausencia.getExpiraEm()).isNull();
        assertThat(ausencia.getOrigemColeta()).isEqualTo(OrigemColeta.KABUM_API);
        assertThat(ausencia.getColetadoEm().toInstant()).isEqualTo(instanteRodada2.toInstant());
        assertThat(precosDa("111")).hasSize(1);
    }

    @Test
    void ausencia_deveTirarOfertaAusenteDaMelhorOfertaAtualEManterAQueVoltou() {
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, true));

        avancar(Duration.ofHours(6));
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true));

        assertThat(ofertasNaMelhorOfertaAtual())
                .contains(ofertaPorIdentificador("111").getId())
                .doesNotContain(ofertaPorIdentificador("222").getId());
    }

    @Test
    void ausencia_deveVoltarParaMelhorOfertaAtualQuandoReaparecerPeloMesmoPreco() {
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, true));

        avancar(Duration.ofHours(6));
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true));

        avancar(Duration.ofHours(6));
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, true));

        List<PrecoEntity> precosDaQueVoltou = precosDa("222");
        assertThat(precosDaQueVoltou).hasSize(3);
        assertThat(precosDaQueVoltou.getLast().isDisponivel()).isTrue();
        assertThat(precosDaQueVoltou.getLast().getValorCentavos()).isEqualTo(20000L);
        assertThat(ofertasNaMelhorOfertaAtual()).contains(ofertaPorIdentificador("222").getId());
    }

    @Test
    void ausencia_naoDeveGravarLinhaNovaQuandoOfertaContinuarAusenteNaRodadaSeguinte() {
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, true));

        avancar(Duration.ofHours(6));
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true));

        avancar(Duration.ofHours(6));
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true));

        assertThat(precosDa("222")).hasSize(2);
    }

    @Test
    void ausencia_naoDeveGravarLinhaQuandoOfertaJaVeioIndisponivelDoColetorEDepoisSumir() {
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, false));

        avancar(Duration.ofHours(6));
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true));

        assertThat(precosDa("222")).hasSize(1);
    }

    @Test
    void ausencia_naoDeveSerConcluidaEmRodadaDeAmostra() {
        gravar(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, true));

        avancar(Duration.ofHours(6));
        gravar(pecaKabum("111", 10000L, true));

        assertThat(precosDa("222")).hasSize(1);
    }

    @Test
    void ausencia_naoDeveSerConcluidaEmCensoDegradadoMasDeveGravarOQueVeio() {
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, true));

        avancar(Duration.ofHours(6));
        VereditoRodada veredito = gravarCenso(CodigoLoja.KABUM, new Reconciliacao(null, 100, 95), 0,
                List.of(pecaKabum("111", 9000L, true)));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.DEGRADADA);
        assertThat(precosDa("111")).hasSize(2);
        assertThat(precosDa("222")).hasSize(1);
    }

    @Test
    void ausencia_naoDeveSerConcluidaEmCensoSaudavelComItemSemPreco() {
        gravarCensoLimpoDaKabum(pecasKabum(10).toArray(OfertaColetada[]::new));

        avancar(Duration.ofHours(6));
        VereditoRodada veredito = gravarCenso(CodigoLoja.KABUM, new Reconciliacao(10L, 10, 10), 1,
                pecasKabum(9));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
        assertThat(precosDa("peca-9")).hasSize(1);
    }

    @Test
    void ausencia_naoDeveSerConcluidaEmCensoSaudavelComDistintosAbaixoDoDeclarado() {
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, true));

        avancar(Duration.ofHours(6));
        VereditoRodada veredito = gravarCenso(CodigoLoja.KABUM, new Reconciliacao(100L, 99, 99), 0,
                List.of(pecaKabum("111", 10000L, true)));

        assertThat(veredito.situacao()).isEqualTo(SituacaoRodada.SAUDAVEL);
        assertThat(precosDa("222")).hasSize(1);
    }

    @Test
    void ausencia_naoDeveTocarOfertaDeLojaForaDoCenso() {
        gravar(oferta(CodigoLoja.STEAM, "730", TipoProduto.JOGO, "Counter-Strike 2",
                "gid-cs2", 8990L, true, OrigemColeta.STEAM_API));
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, true));

        avancar(Duration.ofHours(6));
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true));

        assertThat(precosDa("730")).hasSize(1);
        assertThat(precosDa("222")).hasSize(2);
    }

    @Test
    void ausencia_naoDeveTocarOfertaInativaDaLojaDoCenso() {
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, true));
        ofertaPorIdentificador("222").desativar(OffsetDateTime.now(clock));

        avancar(Duration.ofHours(6));
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true));

        assertThat(precosDa("222")).hasSize(1);
    }

    @Test
    void ausencia_deveManterOfertaAtivaQuandoMarcarIndisponivel() {
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true), pecaKabum("222", 20000L, true));

        avancar(Duration.ofHours(6));
        gravarCensoLimpoDaKabum(pecaKabum("111", 10000L, true));

        assertThat(precosDa("222").getLast().isDisponivel()).isFalse();
        assertThat(ofertaPorIdentificador("222").isAtiva()).isTrue();
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
