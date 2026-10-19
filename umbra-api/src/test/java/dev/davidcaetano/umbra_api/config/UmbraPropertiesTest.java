package dev.davidcaetano.umbra_api.config;


import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

public class UmbraPropertiesTest {

    private static final String CONTATO_VALIDO = "umbra.coleta.contato=contato-de-teste@umbra.invalid";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(contexto -> contexto.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(UmbraProperties.class)
    @Configuration
    static class TestConfig {
    }

    @Test
    void deveSubirComConfigValida() {
        contextRunner.withPropertyValues(
                        "umbra.jwt.secret=secret-testzando",
                        "umbra.jwt.ttl-horas=2",
                        CONTATO_VALIDO)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void deveFalharComSecretAusente() {
        contextRunner
                .withPropertyValues("umbra.jwt.ttl-horas=2", CONTATO_VALIDO)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("secret");
                });
    }

    @Test
    void deveFalharComTTLInvalido() {
        contextRunner
                .withPropertyValues("umbra.jwt.secret=secret-valido",
                        "umbra.jwt.ttl-horas=0",
                        CONTATO_VALIDO)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("ttl-horas");
                });
    }

    @Test
    void falhaComBlocoJwtAusente() {
        contextRunner
                .withPropertyValues()
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("jwt");
                });
    }

    @Test
    void deveFalharComContatoAusente() {
        contextRunner
                .withPropertyValues("umbra.jwt.secret=secret-valido", "umbra.jwt.ttl-horas=2")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("contato");
                });
    }

    @Test
    void deveFalharComContatoQueNaoEhEmail() {
        contextRunner
                .withPropertyValues("umbra.jwt.secret=secret-valido",
                        "umbra.jwt.ttl-horas=2",
                        "umbra.coleta.contato=nao-eh-email")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("contato");
                });
    }
}
