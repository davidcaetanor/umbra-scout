package dev.davidcaetano.umbra_api.coleta.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class AusenciaColetaPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(contexto -> contexto.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(AusenciaColetaProperties.class)
    @Configuration
    static class TestConfig {
    }

    @Test
    void deveCarregarOTetoPercentualDeclarado() {
        contextRunner
                .withPropertyValues("umbra.coleta.ausencia.teto-percentual=20")
                .run(context -> assertThat(context.getBean(AusenciaColetaProperties.class).tetoPercentual())
                        .isEqualTo(20));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void deveFalharComTetoForaDaFaixa(int teto) {
        contextRunner
                .withPropertyValues("umbra.coleta.ausencia.teto-percentual=" + teto)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.ausencia.teto-percentual");
                });
    }

    @Test
    void deveFalharSemTetoPercentual() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.ausencia.teto-percentual");
                });
    }
}
