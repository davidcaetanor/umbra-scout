package dev.davidcaetano.umbra_api.usuario.viacep;

import dev.davidcaetano.umbra_api.comum.WireMockIntegrationTestBase;
import dev.davidcaetano.umbra_api.usuario.service.ConsultaDeCep;
import dev.davidcaetano.umbra_api.usuario.service.ConsultaDeCepIndisponivelException;
import dev.davidcaetano.umbra_api.usuario.service.RegiaoDoCep;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpServerErrorException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ViaCepConsultaDeCepTest extends WireMockIntegrationTestBase {

    private static final String CEP = "01310100";
    private static final String CEP_INEXISTENTE = "99999999";
    private static final String CAMINHO = "/ws/" + CEP + "/json/";

    @Autowired
    private ConsultaDeCep consultaDeCep;

    @Test
    void cepEncontrado_deveDevolverCidadeEUf() {
        WIREMOCK.stubFor(get(urlEqualTo(CAMINHO))
                .willReturn(okJson(fixture("wiremock/viacep/ws-01310100-json.json"))));

        assertThat(consultaDeCep.consultar(CEP)).contains(new RegiaoDoCep("São Paulo", "SP"));
        WIREMOCK.verify(getRequestedFor(urlEqualTo(CAMINHO)));
    }

    @Test
    void cepInexistente_deveDevolverVazioSemFalhar() {
        WIREMOCK.stubFor(get(urlEqualTo("/ws/" + CEP_INEXISTENTE + "/json/"))
                .willReturn(okJson(fixture("wiremock/viacep/ws-99999999-json.json"))));

        assertThat(consultaDeCep.consultar(CEP_INEXISTENTE)).isEmpty();
    }

    @Test
    void erroDoServidor_deveLancarIndisponivelSemOCepNaMensagem() {
        WIREMOCK.stubFor(get(urlEqualTo(CAMINHO)).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> consultaDeCep.consultar(CEP))
                .isInstanceOf(ConsultaDeCepIndisponivelException.class)
                .hasMessageNotContaining(CEP)
                .hasCauseInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void requisicaoRecusada_deveLancarIndisponivel() {
        WIREMOCK.stubFor(get(urlEqualTo(CAMINHO))
                .willReturn(aResponse().withStatus(400).withHeader("Content-Type", "text/html")
                        .withBody("<h1>Http 400</h1>")));

        assertThatThrownBy(() -> consultaDeCep.consultar(CEP))
                .isInstanceOf(ConsultaDeCepIndisponivelException.class);
    }

    @Test
    void respostaAlemDoTimeoutDeLeitura_deveLancarIndisponivel() {
        WIREMOCK.stubFor(get(urlEqualTo(CAMINHO))
                .willReturn(okJson(fixture("wiremock/viacep/ws-01310100-json.json")).withFixedDelay(3_000)));

        assertThatThrownBy(() -> consultaDeCep.consultar(CEP))
                .isInstanceOf(ConsultaDeCepIndisponivelException.class)
                .hasMessageNotContaining(CEP);
    }

    @Test
    void ufForaDoFormato_deveLancarIndisponivelEmVezDeGravarDadoSujo() {
        WIREMOCK.stubFor(get(urlEqualTo(CAMINHO))
                .willReturn(okJson("{\"localidade\": \"São Paulo\", \"uf\": \"sp\"}")));

        assertThatThrownBy(() -> consultaDeCep.consultar(CEP))
                .isInstanceOf(ConsultaDeCepIndisponivelException.class)
                .hasMessageContaining("fora do contrato");
    }

    @Test
    void cidadeAusenteOuAcimaDoLimiteDaColuna_deveLancarIndisponivel() {
        WIREMOCK.stubFor(get(urlEqualTo(CAMINHO))
                .willReturn(okJson("{\"localidade\": \" \", \"uf\": \"SP\"}")));
        assertThatThrownBy(() -> consultaDeCep.consultar(CEP))
                .isInstanceOf(ConsultaDeCepIndisponivelException.class);

        WIREMOCK.stubFor(get(urlEqualTo(CAMINHO))
                .willReturn(okJson("{\"localidade\": \"" + "a".repeat(121) + "\", \"uf\": \"SP\"}")));
        assertThatThrownBy(() -> consultaDeCep.consultar(CEP))
                .isInstanceOf(ConsultaDeCepIndisponivelException.class);
    }

    @Test
    void cidadeNoLimiteDaColuna_deveSerAceita() {
        String cidadeNoLimite = "a".repeat(120);
        WIREMOCK.stubFor(get(urlEqualTo(CAMINHO))
                .willReturn(okJson("{\"localidade\": \" " + cidadeNoLimite + " \", \"uf\": \"SP\"}")));

        assertThat(consultaDeCep.consultar(CEP)).contains(new RegiaoDoCep(cidadeNoLimite, "SP"));
    }

    @Test
    void corpoVazio_deveLancarIndisponivel() {
        WIREMOCK.stubFor(get(urlEqualTo(CAMINHO)).willReturn(aResponse().withStatus(200)));

        assertThatThrownBy(() -> consultaDeCep.consultar(CEP))
                .isInstanceOf(ConsultaDeCepIndisponivelException.class)
                .hasMessageContaining("sem corpo");
    }
}
