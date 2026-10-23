package dev.davidcaetano.umbra_api.coleta.properties;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties.Limiares;
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

class AlarmeColetaPropertiesTest {

    private static final Limiares LIMIARES_PADRAO = new Limiares(10, 20, 2, 2);

    private static final String[] PADRAO_VALIDO = {
            "umbra.coleta.alarme.padrao.amostra-minima=10",
            "umbra.coleta.alarme.padrao.limiar-sem-preco-percentual=20",
            "umbra.coleta.alarme.padrao.margem-cobertura-percentual=2",
            "umbra.coleta.alarme.padrao.margem-duplicata-percentual=2"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(contexto -> contexto.getEnvironment().getPropertySources()
                    .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
            .withUserConfiguration(TestConfig.class);

    @EnableConfigurationProperties(AlarmeColetaProperties.class)
    @Configuration
    static class TestConfig {
    }

    @Test
    void deveDevolverOPadraoParaQualquerFonteQuandoNaoHouverSobrescrita() {
        contextRunner
                .withPropertyValues(PADRAO_VALIDO)
                .run(context -> {
                    AlarmeColetaProperties alarme = context.getBean(AlarmeColetaProperties.class);

                    assertThat(alarme.porFonte()).isEmpty();
                    assertThat(alarme.limiaresDe(OrigemColeta.STEAM_API)).isEqualTo(LIMIARES_PADRAO);
                    assertThat(alarme.limiaresDe(OrigemColeta.KABUM_API)).isEqualTo(LIMIARES_PADRAO);
                });
    }

    @Test
    void deveSobrescreverSoOCampoDeclaradoParaAFonte() {
        contextRunner
                .withPropertyValues(PADRAO_VALIDO)
                .withPropertyValues("umbra.coleta.alarme.por-fonte.steam-api.margem-duplicata-percentual=10")
                .run(context -> {
                    AlarmeColetaProperties alarme = context.getBean(AlarmeColetaProperties.class);

                    assertThat(alarme.limiaresDe(OrigemColeta.STEAM_API)).isEqualTo(new Limiares(10, 20, 2, 10));
                    assertThat(alarme.limiaresDe(OrigemColeta.KABUM_API)).isEqualTo(LIMIARES_PADRAO);
                });
    }

    @Test
    void deveSobrescreverOPadraoPorVariavelDeAmbiente() {
        contextRunner
                .withInitializer(comVariavelDeAmbiente("UMBRA_COLETA_ALARME_PADRAO_LIMIARSEMPRECOPERCENTUAL", "35"))
                .withPropertyValues(PADRAO_VALIDO)
                .run(context -> assertThat(context.getBean(AlarmeColetaProperties.class).padrao())
                        .isEqualTo(new Limiares(10, 35, 2, 2)));
    }

    @Test
    void deveCriarSobrescritaDaFontePorVariavelDeAmbiente() {
        contextRunner
                .withInitializer(comVariavelDeAmbiente(
                        "UMBRA_COLETA_ALARME_PORFONTE_STEAMAPI_MARGEMDUPLICATAPERCENTUAL", "10"))
                .withPropertyValues(PADRAO_VALIDO)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AlarmeColetaProperties.class).limiaresDe(OrigemColeta.STEAM_API))
                            .isEqualTo(new Limiares(10, 20, 2, 10));
                });
    }

    @Test
    void deveFalharComPercentualAcimaDe100NoPadrao() {
        contextRunner
                .withPropertyValues(PADRAO_VALIDO)
                .withPropertyValues("umbra.coleta.alarme.padrao.limiar-sem-preco-percentual=101")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.alarme.padrao.limiar-sem-preco-percentual");
                });
    }

    @Test
    void deveFalharComAmostraMinimaZeroNoPadrao() {
        contextRunner
                .withPropertyValues(PADRAO_VALIDO)
                .withPropertyValues("umbra.coleta.alarme.padrao.amostra-minima=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.alarme.padrao.amostra-minima");
                });
    }

    @Test
    void deveFalharComPercentualNegativoNaSobrescrita() {
        contextRunner
                .withPropertyValues(PADRAO_VALIDO)
                .withPropertyValues("umbra.coleta.alarme.por-fonte.steam-api.limiar-sem-preco-percentual=-1")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.alarme.por-fonte")
                            .hasMessageContaining("limiar-sem-preco-percentual");
                });
    }

    @Test
    void deveFalharSemPadrao() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("umbra.coleta.alarme.padrao");
                });
    }

    private static ApplicationContextInitializer<ConfigurableApplicationContext> comVariavelDeAmbiente(String nome, String valor) {
        return contexto -> contexto.getEnvironment().getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(nome, valor)));
    }
}
