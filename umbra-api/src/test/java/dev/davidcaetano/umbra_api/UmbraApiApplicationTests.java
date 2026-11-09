package dev.davidcaetano.umbra_api;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.properties.BuscaProperties;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties.Limiares;
import dev.davidcaetano.umbra_api.coleta.properties.AusenciaColetaProperties;
import dev.davidcaetano.umbra_api.coleta.properties.GravacaoPrecoProperties;
import dev.davidcaetano.umbra_api.coleta.properties.ItadColetaProperties;
import dev.davidcaetano.umbra_api.coleta.itad.ItadBuscaClient;
import dev.davidcaetano.umbra_api.coleta.itad.ItadClientImpl;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UmbraApiApplicationTests extends IntegrationTestBase {

	@Autowired
	private AlarmeColetaProperties alarme;

	@Autowired
	private GravacaoPrecoProperties gravacao;

	@Autowired
	private ItadColetaProperties itadColeta;

	@Autowired
	private AusenciaColetaProperties ausencia;

	@Autowired
	private RateLimiterRegistry rateLimiterRegistry;

	@Test
	void contextLoads() {
	}

	@Test
	void deveCarregarOsLimiaresEAJanelaDeVivacidadeComOsValoresDeHoje() {
		assertThat(alarme.limiaresDe(OrigemColeta.KABUM_API)).isEqualTo(new Limiares(10, 20, 2, 2));
		assertThat(alarme.limiaresDe(OrigemColeta.ITAD_API)).isEqualTo(new Limiares(10, 20, 2, 2));
		assertThat(gravacao.janelaVivacidadeDias()).isEqualTo(90);
	}

	@Test
	void deveSobrescreverSoAAmostraMinimaEOLimiarSemPrecoDaSteam() {
		assertThat(alarme.porFonte()).containsOnlyKeys(OrigemColeta.STEAM_API);
		assertThat(alarme.limiaresDe(OrigemColeta.STEAM_API)).isEqualTo(new Limiares(5, 40, 2, 2));
	}

	@Test
	void deveLimitarAAusenciaPorCensoA20PorCentoDoEscopo() {
		assertThat(ausencia.tetoPercentual()).isEqualTo(20);
	}

	@Test
	void deveManterOOrcamentoDaDescobertaDoItadEm30Paginas() {
		assertThat(itadColeta.paginasPorRodada()).isEqualTo(30);
	}

	@Test
	void deveDividirACotaDoItadEntreColetaEBusca_semInstanciaOrfa() {
		assertThat(rateLimiterRegistry.find("itad")).isEmpty();
		assertThat(nomesDeRateLimiter(ItadClientImpl.class)).containsOnly("itad-coleta");
		assertThat(nomesDeRateLimiter(ItadBuscaClient.class)).containsOnly("itad-busca");

		RateLimiterConfig coleta = rateLimiterRegistry.rateLimiter("itad-coleta").getRateLimiterConfig();
		RateLimiterConfig busca = rateLimiterRegistry.rateLimiter("itad-busca").getRateLimiterConfig();
		assertThat(coleta.getLimitForPeriod()).isEqualTo(600);
		assertThat(coleta.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(2));
		assertThat(busca.getLimitForPeriod()).isEqualTo(800);
		assertThat(busca.getTimeoutDuration()).isZero();
		assertThat(Stream.of(coleta, busca))
				.allSatisfy(config -> assertThat(config.getLimitRefreshPeriod()).isEqualTo(Duration.ofMinutes(5)));
	}

	@Test
	void deveDeclararABuscaExternaComOsValoresCalibrados() throws IOException {
		List<PropertySource<?>> yamlDeProducao = new YamlPropertySourceLoader()
				.load("application.yaml", new ClassPathResource("application.yaml"));
		BuscaProperties.Externa externa = new Binder(ConfigurationPropertySources.from(yamlDeProducao))
				.bind("umbra.busca", BuscaProperties.class)
				.get()
				.externa();

		assertThat(externa).isEqualTo(new BuscaProperties.Externa(400, 15, Duration.ofMinutes(5),
				Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofMinutes(6), Duration.ofSeconds(4), 5));
	}

	private static Stream<String> nomesDeRateLimiter(Class<?> cliente) {
		return Arrays.stream(cliente.getDeclaredMethods())
				.map(metodo -> metodo.getAnnotation(RateLimiter.class))
				.filter(Objects::nonNull)
				.map(RateLimiter::name);
	}

}
