package dev.davidcaetano.umbra_api;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties.Limiares;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Arrays;
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
		assertThat(busca.getLimitForPeriod()).isEqualTo(400);
		assertThat(Stream.of(coleta, busca)).allSatisfy(config -> {
			assertThat(config.getLimitRefreshPeriod()).isEqualTo(Duration.ofMinutes(5));
			assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(2));
		});
	}

	private static Stream<String> nomesDeRateLimiter(Class<?> cliente) {
		return Arrays.stream(cliente.getDeclaredMethods())
				.map(metodo -> metodo.getAnnotation(RateLimiter.class))
				.filter(Objects::nonNull)
				.map(RateLimiter::name);
	}

}
