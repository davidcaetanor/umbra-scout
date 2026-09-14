package dev.davidcaetano.umbra_api.coleta;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroMoedaTest {

    private final FiltroMoeda filtroMoeda = new FiltroMoeda();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void configurarAppender() {
        logger = (Logger) LoggerFactory.getLogger(FiltroMoeda.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void removerAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void aceita_deveRetornarTrueParaBrl() {
        assertThat(filtroMoeda.aceita("BRL")).isTrue();
    }

    @Test
    void aceita_deveRetornarTrueQuandoTodosArgumentosForemBrl() {
        assertThat(filtroMoeda.aceita("BRL", "BRL", "BRL")).isTrue();
    }

    @Test
    void aceita_deveRetornarFalseQuandoAlgumArgumentoForDiferenteDeBrl() {
        assertThat(filtroMoeda.aceita("BRL", "USD")).isFalse();
    }

    @Test
    void aceita_deveRetornarFalseParaArgumentoNulo() {
        assertThat(filtroMoeda.aceita((String) null)).isFalse();
    }

    @Test
    void registrarDescarte_naoDeveGuardarBrlNoConjuntoDeMoedasVistas() {
        filtroMoeda.registrarDescarte("BRL", "USD");

        filtroMoeda.logarResumo(OrigemColeta.STEAM_API);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getFormattedMessage())
                .contains("moedas vistas: [USD])");
    }

    @Test
    void registrarDescarte_deveContarOfertasNaoMoedas() {
        filtroMoeda.registrarDescarte("USD", "EUR");
        filtroMoeda.registrarDescarte("USD", "GBP");

        filtroMoeda.logarResumo(OrigemColeta.STEAM_API);

        assertThat(appender.list.getFirst().getFormattedMessage()).contains("2 ofertas");
    }

    @Test
    void logarResumo_naoDeveEmitirNadaQuandoNenhumDescarteForRegistrado() {
        filtroMoeda.logarResumo(OrigemColeta.STEAM_API);

        assertThat(appender.list).isEmpty();
    }
}
