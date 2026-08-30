package dev.davidcaetano.umbra_api.config;


import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class UmbraPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(UmbraProperties.class)
    @Configuration
    static class TestConfig {
    }

    @Test
    void deveSubirComConfigValida() {
        contextRunner.withPropertyValues(
                        "umbra.jwt.secret=secret-testzando",
                        "umbra.jwt.ttl-horas=2")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void deveFalharComSecretAusente() {
        contextRunner
                .withPropertyValues("umbra.jwt.ttl-horas=2")
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
                        "umbra.jwt.ttl-horas=0")
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
}
