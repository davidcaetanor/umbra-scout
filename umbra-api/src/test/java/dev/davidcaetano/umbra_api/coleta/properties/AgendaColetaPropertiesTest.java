package dev.davidcaetano.umbra_api.coleta.properties;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgendaColetaPropertiesTest {

    private static final String CRON_ITAD = "0 0 0,6,12,18 * * *";
    private static final String CRON_ITAD_DA_VARIAVEL = "0 10 1,7,13,19 * * *";
    private static final String DESCOBERTA_ITAD_VALIDA = "umbra.coleta.agenda.descoberta.itad-api=" + CRON_ITAD;
    private static final String REFRESH_VALIDO = "umbra.coleta.agenda.refresh=0 30 0,6,12,18 * * *";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(contexto -> contexto.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(AgendaColetaProperties.class)
    @Configuration
    static class TestConfig {
    }

    @Test
    void deveConverterChaveDaAgendaParaOrigemColeta() {
        contextRunner
                .withPropertyValues(DESCOBERTA_ITAD_VALIDA, REFRESH_VALIDO)
                .run(context -> assertThat(context.getBean(AgendaColetaProperties.class).descoberta())
                        .containsOnlyKeys(OrigemColeta.ITAD_API)
                        .containsEntry(OrigemColeta.ITAD_API, CRON_ITAD));
    }

    @Test
    void deveSobrescreverCronDaDescobertaPorVariavelDeAmbiente() {
        contextRunner
                .withInitializer(comVariavelDeAmbiente("UMBRA_COLETA_AGENDA_DESCOBERTA_ITAD_API", CRON_ITAD_DA_VARIAVEL))
                .withPropertyValues(REFRESH_VALIDO)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AgendaColetaProperties.class).descoberta())
                            .containsOnlyKeys(OrigemColeta.ITAD_API)
                            .containsEntry(OrigemColeta.ITAD_API, CRON_ITAD_DA_VARIAVEL);
                });
    }

    @Test
    void deveDeixarRodadaInicialDesligadaQuandoChaveAusente() {
        contextRunner
                .withPropertyValues(DESCOBERTA_ITAD_VALIDA, REFRESH_VALIDO)
                .run(context -> assertThat(context.getBean(AgendaColetaProperties.class).rodadaInicial()).isFalse());
    }

    @Test
    void deveLigarRodadaInicialPorVariavelDeAmbiente() {
        contextRunner
                .withInitializer(comVariavelDeAmbiente("UMBRA_COLETA_AGENDA_RODADAINICIAL", "true"))
                .withPropertyValues(DESCOBERTA_ITAD_VALIDA, REFRESH_VALIDO)
                .run(context -> assertThat(context.getBean(AgendaColetaProperties.class).rodadaInicial()).isTrue());
    }

    @Test
    void deveFalharComChaveDaAgendaQueNaoEhOrigemColeta() {
        contextRunner
                .withPropertyValues("umbra.coleta.agenda.descoberta.loja-inexistente=" + CRON_ITAD, REFRESH_VALIDO)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("umbra.coleta.agenda.descoberta")
                            .hasStackTraceContaining("loja-inexistente");
                });
    }

    @Test
    void deveFalharComRefreshEmBranco() {
        contextRunner
                .withPropertyValues(DESCOBERTA_ITAD_VALIDA, "umbra.coleta.agenda.refresh= ")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.agenda.refresh");
                });
    }

    @Test
    void deveFalharComDescobertaVazia() {
        contextRunner
                .withPropertyValues(REFRESH_VALIDO)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.agenda.descoberta");
                });
    }

    private static ApplicationContextInitializer<ConfigurableApplicationContext> comVariavelDeAmbiente(String nome, String valor) {
        return contexto -> contexto.getEnvironment().getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(nome, valor)));
    }
}
