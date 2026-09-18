package dev.davidcaetano.umbra_api.coleta.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.PrecoRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoRepository;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ColetaServiceChunkIsolationTest extends IntegrationTestBase {

    @Autowired
    private ColetaService coletaService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private PrecoRepository precoRepository;

    @AfterEach
    void limparBanco() {
        precoRepository.deleteAll();
        ofertaRepository.deleteAll();
        produtoRepository.deleteAll();
    }

    private OfertaColetada ofertaValida(String identificadorLoja) {
        return new OfertaColetada(CodigoLoja.KABUM, identificadorLoja, TipoProduto.HARDWARE,
                "Peça " + identificadorLoja, "Hardware", "https://loja.exemplo/" + identificadorLoja,
                null, null, 10000L, null, null, true, OrigemColeta.KABUM_API, null);
    }

    private OfertaColetada ofertaComUrlInvalida(String identificadorLoja) {
        return new OfertaColetada(CodigoLoja.KABUM, identificadorLoja, TipoProduto.HARDWARE,
                "Peça " + identificadorLoja, "Hardware", "url-sem-esquema",
                null, null, 10000L, null, null, true, OrigemColeta.KABUM_API, null);
    }

    private static ListAppender<ILoggingEvent> capturarLogsDoServico() {
        Logger logger = (Logger) LoggerFactory.getLogger(ColetaServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void pararDeCapturar(ListAppender<ILoggingEvent> appender) {
        appender.stop();
        ((Logger) LoggerFactory.getLogger(ColetaServiceImpl.class)).detachAppender(appender);
    }

    @Test
    void devePersistirChunkAnteriorQuandoUmChunkPosteriorFalhar() {
        ListAppender<ILoggingEvent> logs = capturarLogsDoServico();

        try {
            List<OfertaColetada> ofertas = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                ofertas.add(ofertaValida("valida-" + i));
            }
            ofertas.add(ofertaComUrlInvalida("invalida-500"));

            assertThatCode(() -> coletaService.gravar(new ResultadoColeta(ofertas, ofertas.size(), 0,
                    new Reconciliacao(null, ofertas.size(), ofertas.size()))))
                    .doesNotThrowAnyException();

            assertThat(produtoRepository.count()).isEqualTo(500);
            assertThat(ofertaRepository.count()).isEqualTo(500);

            assertThat(logs.list).anySatisfy(evento -> {
                assertThat(evento.getLevel()).isEqualTo(Level.WARN);
                assertThat(evento.getFormattedMessage()).contains("Falha ao gravar chunk");
            });
        } finally {
            pararDeCapturar(logs);
        }
    }

    @Test
    void deveLancarQuandoTodosOsChunksFalharem() {
        ListAppender<ILoggingEvent> logs = capturarLogsDoServico();

        try {
            List<OfertaColetada> ofertas = List.of(ofertaComUrlInvalida("unica-invalida"));

            assertThatThrownBy(() -> coletaService.gravar(new ResultadoColeta(ofertas, ofertas.size(), 0,
                    new Reconciliacao(null, ofertas.size(), ofertas.size()))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Nenhum dos 1 chunk");

            assertThat(produtoRepository.count()).isZero();

            assertThat(logs.list).anySatisfy(evento -> assertThat(evento.getLevel()).isEqualTo(Level.ERROR));
        } finally {
            pararDeCapturar(logs);
        }
    }
}
