package dev.davidcaetano.umbra_api.coleta.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ColetaServiceSobDemandaTest extends IntegrationTestBase {

    @Autowired
    private ColetaService coletaService;

    @Autowired
    private JdbcTemplate jdbc;

    private final String gid = UUID.randomUUID().toString();

    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void capturarLogs() {
        logs = new ListAppender<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(ColetaService.class)).addAppender(logs);
        ((Logger) LoggerFactory.getLogger(AusenciaPorCenso.class)).addAppender(logs);
    }

    @AfterEach
    void limpar() {
        ((Logger) LoggerFactory.getLogger(ColetaService.class)).detachAppender(logs);
        ((Logger) LoggerFactory.getLogger(AusenciaPorCenso.class)).detachAppender(logs);
        jdbc.update("""
                DELETE FROM preco WHERE oferta_id IN (
                    SELECT o.id FROM oferta o JOIN produto p ON p.id = o.produto_id
                     WHERE p.chave_itad = ? OR p.nome = 'Vizinho Sob Demanda')
                """, gid);
        jdbc.update("""
                DELETE FROM oferta WHERE produto_id IN (
                    SELECT id FROM produto WHERE chave_itad = ? OR nome = 'Vizinho Sob Demanda')
                """, gid);
        jdbc.update("DELETE FROM produto WHERE chave_itad = ? OR nome = 'Vizinho Sob Demanda'", gid);
    }

    private OfertaColetada oferta(CodigoLoja loja, long valor) {
        return new OfertaColetada(loja, "id-" + loja + "-" + gid, TipoProduto.JOGO, "Hollow Knight", null,
                "https://loja.exemplo.com/hk", "https://assets.exemplo.com/hk.jpg", gid, valor, valor, (short) 0,
                true, OrigemColeta.ITAD_API, null);
    }

    private int contar(String sql) {
        return jdbc.queryForObject(sql, Integer.class, gid);
    }

    private int produtos() {
        return contar("SELECT count(*) FROM produto WHERE chave_itad = ?");
    }

    private int ofertas() {
        return contar("SELECT count(*) FROM oferta o JOIN produto p ON p.id = o.produto_id WHERE p.chave_itad = ?");
    }

    private int precos() {
        return contar("""
                SELECT count(*) FROM preco pr JOIN oferta o ON o.id = pr.oferta_id
                  JOIN produto p ON p.id = o.produto_id WHERE p.chave_itad = ?
                """);
    }

    private List<String> mensagens() {
        return logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    void deveCriarProdutoOfertasEPrimeiroPrecoSemJulgarRodadaNemConcluirAusencia() {
        Long vizinho = jdbc.queryForObject(
                "INSERT INTO produto (tipo, nome) VALUES ('JOGO', 'Vizinho Sob Demanda') RETURNING id", Long.class);
        Long ofertaVizinha = jdbc.queryForObject("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url)
                VALUES (?, (SELECT id FROM loja WHERE codigo = 'GOG'), ?, 'https://www.gog.com/vizinho')
                RETURNING id
                """, Long.class, vizinho, "vizinho-" + gid);
        jdbc.update("INSERT INTO preco (oferta_id, valor_centavos, disponivel, origem_coleta) VALUES (?, 1000, TRUE, 'ITAD_API')",
                ofertaVizinha);

        coletaService.gravarSobDemanda(List.of(oferta(CodigoLoja.GOG, 4500), oferta(CodigoLoja.GREEN_MAN_GAMING, 3900)));

        assertThat(produtos()).isEqualTo(1);
        assertThat(ofertas()).isEqualTo(2);
        assertThat(precos()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM preco WHERE oferta_id = ?", Integer.class, ofertaVizinha))
                .isEqualTo(1);
        assertThat(mensagens()).noneMatch(mensagem -> mensagem.startsWith("Rodada medida"));
        assertThat(mensagens()).anyMatch(mensagem -> mensagem.startsWith("Gravacao sob demanda: 1 produto(s) criado(s)"));
    }

    @Test
    void segundaGravacaoDoMesmoJogoNaoDeveDuplicarNada() {
        List<OfertaColetada> ofertas = List.of(oferta(CodigoLoja.GOG, 4500));

        coletaService.gravarSobDemanda(ofertas);
        coletaService.gravarSobDemanda(ofertas);

        assertThat(produtos()).isEqualTo(1);
        assertThat(ofertas()).isEqualTo(1);
        assertThat(precos()).isEqualTo(1);
    }

    @Test
    void listaVaziaNaoDeveGravarNemLogar() {
        coletaService.gravarSobDemanda(List.of());

        assertThat(mensagens()).isEmpty();
    }

    @Test
    void duasGravacoesSimultaneasDoMesmoJogoDevemTerminarComUmProdutoSoESemErro() throws Exception {
        List<OfertaColetada> ofertas = List.of(oferta(CodigoLoja.GOG, 4500), oferta(CodigoLoja.GREEN_MAN_GAMING, 3900));
        CyclicBarrier largada = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CompletableFuture<Void> primeira = CompletableFuture.runAsync(() -> gravarJuntos(largada, ofertas), executor);
            CompletableFuture<Void> segunda = CompletableFuture.runAsync(() -> gravarJuntos(largada, ofertas), executor);

            CompletableFuture.allOf(primeira, segunda).get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(produtos()).isEqualTo(1);
        assertThat(ofertas()).isEqualTo(2);
        assertThat(precos()).isEqualTo(2);
    }

    private void gravarJuntos(CyclicBarrier largada, List<OfertaColetada> ofertas) {
        try {
            largada.await(10, TimeUnit.SECONDS);
        } catch (Exception interrupcao) {
            throw new IllegalStateException(interrupcao);
        }
        coletaService.gravarSobDemanda(ofertas);
    }
}
