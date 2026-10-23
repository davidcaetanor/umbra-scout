package dev.davidcaetano.umbra_api.coleta.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class GravacaoPrecoPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(contexto -> contexto.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(GravacaoPrecoProperties.class)
    @Configuration
    static class TestConfig {
    }

    @Test
    void deveLigarAJanelaDeVivacidadeDoValorConfigurado() {
        contextRunner
                .withPropertyValues("umbra.coleta.gravacao.janela-vivacidade-dias=45")
                .run(context -> assertThat(context.getBean(GravacaoPrecoProperties.class).janelaVivacidadeDias())
                        .isEqualTo(45));
    }

    @Test
    void deveFalharComJanelaDeVivacidadeZero() {
        contextRunner
                .withPropertyValues("umbra.coleta.gravacao.janela-vivacidade-dias=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.gravacao.janela-vivacidade-dias");
                });
    }
}
