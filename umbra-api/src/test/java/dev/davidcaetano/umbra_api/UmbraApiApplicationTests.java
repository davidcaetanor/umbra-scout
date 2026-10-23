package dev.davidcaetano.umbra_api;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties.Limiares;
import dev.davidcaetano.umbra_api.coleta.properties.GravacaoPrecoProperties;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UmbraApiApplicationTests extends IntegrationTestBase {

	@Autowired
	private AlarmeColetaProperties alarme;

	@Autowired
	private GravacaoPrecoProperties gravacao;

	@Test
	void contextLoads() {
	}

	@Test
	void deveCarregarOsLimiaresEAJanelaDeVivacidadeComOsValoresDeHoje() {
		assertThat(alarme.limiaresDe(OrigemColeta.KABUM_API)).isEqualTo(new Limiares(10, 20, 2, 2));
		assertThat(alarme.porFonte()).isEmpty();
		assertThat(gravacao.janelaVivacidadeDias()).isEqualTo(90);
	}

}
