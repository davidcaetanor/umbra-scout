package dev.davidcaetano.umbra_api.config;


import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

public class UmbraPropertiesTest {

    private static final String SEGREDO_VALIDO = "umbra.jwt.secret=" + "s".repeat(32);
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
                        SEGREDO_VALIDO,
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
                            .hasMessageContaining("umbra.jwt.secret");
                });
    }

    @Test
    void deveFalharComSecretAbaixoDe256Bits() {
        contextRunner
                .withPropertyValues("umbra.jwt.secret=" + "s".repeat(31), "umbra.jwt.ttl-horas=2", CONTATO_VALIDO)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.jwt.secret deve ter pelo menos 32 caracteres");
                });
    }

    @Test
    void semOrigemDeCors_deveSubirSemNenhumaOrigemPermitida() {
        contextRunner.withPropertyValues(SEGREDO_VALIDO, "umbra.jwt.ttl-horas=2", CONTATO_VALIDO)
                .run(context -> assertThat(context.getBean(UmbraProperties.class).cors().origens()).isEmpty());
    }

    @Test
    void origensDeCorsSeparadasPorVirgula_devemVirarLista() {
        contextRunner.withPropertyValues(SEGREDO_VALIDO, "umbra.jwt.ttl-horas=2", CONTATO_VALIDO,
                        "umbra.cors.origens=https://umbra.exemplo.com,http://localhost:4200")
                .run(context -> assertThat(context.getBean(UmbraProperties.class).cors().origens())
                        .containsExactly("https://umbra.exemplo.com", "http://localhost:4200"));
    }

    @Test
    void origemDeCorsComCuringa_deveFalharNaSubida() {
        contextRunner.withPropertyValues(SEGREDO_VALIDO, "umbra.jwt.ttl-horas=2", CONTATO_VALIDO,
                        "umbra.cors.origens=*")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.cors.origens");
                });
    }

    @Test
    void deveFalharComTTLInvalido() {
        contextRunner
                .withPropertyValues(SEGREDO_VALIDO,
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
    void falhaComBlocoColetaAusente() {
        contextRunner
                .withPropertyValues(SEGREDO_VALIDO, "umbra.jwt.ttl-horas=2")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.contato");
                });
    }

    @Test
    void deveFalharComContatoVazio() {
        contextRunner
                .withPropertyValues(SEGREDO_VALIDO,
                        "umbra.jwt.ttl-horas=2",
                        "umbra.coleta.contato=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.contato e obrigatorio e nao pode estar vazio");
                });
    }

    @Test
    void deveFalharComContatoQueNaoEhEmail() {
        contextRunner
                .withPropertyValues(SEGREDO_VALIDO,
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
