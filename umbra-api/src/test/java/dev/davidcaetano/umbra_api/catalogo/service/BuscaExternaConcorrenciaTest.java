package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.comum.WireMockIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class BuscaExternaConcorrenciaTest extends WireMockIntegrationTestBase {

    private static final String FIXTURE_BUSCA = fixture("wiremock/itad/games-search-v1-elden-ring.json");
    private static final String FIXTURE_PRECOS = fixture("wiremock/itad/games-prices-v3-elden-ring.json");
    private static final int THREADS = 20;
    private static final long LIMITE_DE_ESPERA_SEGUNDOS = 10;

    @Autowired
    private BuscaExternaService buscaExternaService;

    private final ExecutorService executor = Executors.newFixedThreadPool(THREADS);

    @AfterEach
    void encerrarExecutor() {
        executor.shutdownNow();
    }

    @Test
    void vinteBuscasDoMesmoTermo_devemFazerUmaChamadaDeBuscaEUmaDePrecos() throws Exception {
        stubBusca("t212c-conc-um", 300);
        stubPrecos();

        List<List<JogoExterno>> resultados = disparar(THREADS, () -> buscaExternaService.buscar("t212c-conc-um"));

        WIREMOCK.verify(1, getRequestedFor(urlPathEqualTo("/games/search/v1")));
        WIREMOCK.verify(1, postRequestedFor(urlPathEqualTo("/games/prices/v3")));
        assertThat(resultados.getFirst()).hasSize(3);
        assertThat(resultados).allSatisfy(resultado -> assertThat(resultado).isEqualTo(resultados.getFirst()));
    }

    @Test
    void vinteBuscasDoMesmoTermoComItadEm500_devemFazerUmaChamadaEReceberVazio() throws Exception {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/search/v1"))
                .willReturn(aResponse().withStatus(500).withFixedDelay(300)));

        List<List<JogoExterno>> resultados = disparar(THREADS, () -> buscaExternaService.buscar("t212c-conc-500"));

        WIREMOCK.verify(1, getRequestedFor(urlPathEqualTo("/games/search/v1")));
        assertThat(resultados).hasSize(THREADS).allSatisfy(resultado -> assertThat(resultado).isEmpty());

        assertThat(buscaExternaService.buscar("t212c-conc-500")).isEmpty();
        WIREMOCK.verify(2, getRequestedFor(urlPathEqualTo("/games/search/v1")));
    }

    @Test
    void termoLento_naoDeveTravarABuscaDeOutroTermo() throws Exception {
        // Os dois termos têm o mesmo hashCode e caem no mesmo trecho do mapa interno do cache, qualquer que seja
        // o tamanho dele: se a chamada HTTP rodasse dentro da trava do mapa, o termo rápido esperaria o lento.
        // O rápido só sai depois de o lento estar em voo, senão a ordem de chegada à trava decidiria o teste.
        String termoLento = "lento az";
        String termoRapido = "lento b[";
        assertThat(termoLento.hashCode()).isEqualTo(termoRapido.hashCode());
        stubBusca(termoLento, 800);
        stubBusca(termoRapido, 0);
        stubPrecos();

        Future<Long> lento = executor.submit(terminarEm(termoLento));
        aguardarBuscaRecebidaPeloItad(termoLento);
        Future<Long> rapido = executor.submit(terminarEm(termoRapido));

        long fimDoRapido = rapido.get(LIMITE_DE_ESPERA_SEGUNDOS, TimeUnit.SECONDS);
        long fimDoLento = lento.get(LIMITE_DE_ESPERA_SEGUNDOS, TimeUnit.SECONDS);
        assertThat(fimDoRapido).isLessThan(fimDoLento);
    }

    private Callable<Long> terminarEm(String termo) {
        return () -> {
            assertThat(buscaExternaService.buscar(termo)).isNotEmpty();
            return System.nanoTime();
        };
    }

    private static void aguardarBuscaRecebidaPeloItad(String termo) {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(LIMITE_DE_ESPERA_SEGUNDOS);
        while (WIREMOCK.findAll(getRequestedFor(urlPathEqualTo("/games/search/v1"))
                .withQueryParam("title", equalTo(termo))).isEmpty()) {
            assertThat(System.nanoTime()).as("a busca de '%s' não chegou ao ITAD", termo).isLessThan(limite);
            Thread.onSpinWait();
        }
    }

    private List<List<JogoExterno>> disparar(int quantidade, Callable<List<JogoExterno>> busca) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<List<JogoExterno>>> futuros = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            futuros.add(executor.submit(() -> {
                largada.await();
                return busca.call();
            }));
        }
        largada.countDown();

        List<List<JogoExterno>> resultados = new ArrayList<>();
        for (Future<List<JogoExterno>> futuro : futuros) {
            resultados.add(futuro.get(LIMITE_DE_ESPERA_SEGUNDOS, TimeUnit.SECONDS));
        }
        return resultados;
    }

    private static void stubBusca(String termo, int atrasoMs) {
        WIREMOCK.stubFor(get(urlPathEqualTo("/games/search/v1"))
                .withQueryParam("title", equalTo(termo))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_BUSCA).withFixedDelay(atrasoMs)));
    }

    private static void stubPrecos() {
        WIREMOCK.stubFor(post(urlPathEqualTo("/games/prices/v3"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(FIXTURE_PRECOS)));
    }
}
