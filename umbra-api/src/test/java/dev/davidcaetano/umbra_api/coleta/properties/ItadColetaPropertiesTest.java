package dev.davidcaetano.umbra_api.coleta.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class ItadColetaPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(contexto -> contexto.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(ItadColetaProperties.class)
    @Configuration
    static class TestConfig {
    }

    @Test
    void deveCarregarAsPaginasPorRodadaDeclaradas() {
        contextRunner
                .withPropertyValues("umbra.coleta.itad.paginas-por-rodada=2")
                .run(context -> assertThat(context.getBean(ItadColetaProperties.class).paginasPorRodada())
                        .isEqualTo(2));
    }

    @Test
    void deveFalharComPaginasPorRodadaZero() {
        contextRunner
                .withPropertyValues("umbra.coleta.itad.paginas-por-rodada=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.itad.paginas-por-rodada");
                });
    }

    @Test
    void deveFalharSemPaginasPorRodada() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.itad.paginas-por-rodada");
                });
    }
}
