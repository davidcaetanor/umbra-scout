package dev.davidcaetano.umbra_api.catalogo.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class BuscaPropertiesTest {

    private static final String[] EXTERNA_VALIDA = {
            "umbra.busca.externa.teto-global=400",
            "umbra.busca.externa.teto-por-ip=15",
            "umbra.busca.externa.janela=5m",
            "umbra.busca.externa.timeout-conexao=1s",
            "umbra.busca.externa.timeout-leitura=1s",
            "umbra.busca.externa.cache-ttl=2m",
            "umbra.busca.externa.prazo=4s",
            "umbra.busca.externa.maximo-resultados=5"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(contexto -> contexto.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(BuscaProperties.class)
    @Configuration
    static class TestConfig {
    }

    @Test
    void deveCarregarOsDoisTetos() {
        contextRunner
                .withPropertyValues(EXTERNA_VALIDA)
                .run(context -> {
                    BuscaProperties.Externa externa = context.getBean(BuscaProperties.class).externa();
                    assertThat(externa.tetoGlobal()).isEqualTo(400);
                    assertThat(externa.tetoPorIp()).isEqualTo(15);
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"teto-global", "teto-por-ip"})
    void deveFalharComTetoZero(String teto) {
        contextRunner
                .withPropertyValues(EXTERNA_VALIDA)
                .withPropertyValues("umbra.busca.externa." + teto + "=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.busca.externa." + teto);
                });
    }
}
