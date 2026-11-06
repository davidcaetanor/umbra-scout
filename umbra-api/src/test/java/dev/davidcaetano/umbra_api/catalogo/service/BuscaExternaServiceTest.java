package dev.davidcaetano.umbra_api.catalogo.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.properties.BuscaProperties;
import dev.davidcaetano.umbra_api.comum.log.ChaveDeLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BuscaExternaServiceTest {

    private static final Duration CACHE_LONGO = Duration.ofMinutes(5);
    private static final Duration PRAZO = Duration.ofSeconds(4);
    private static final Duration JANELA_LONGA = Duration.ofMinutes(5);
    private static final long LIMITE_DE_ESPERA_SEGUNDOS = 10;
    private static final String AVISO_DE_PRAZO = "Busca externa passou do prazo, só o catálogo local: termo=";
    private static final String CHAMADA_AO_ITAD = "Busca externa no ITAD: ";
    private static final String REQUEST_ID = ChaveDeLog.REQUEST_ID.chave();
    private static final String ID_DA_REQUISICAO = "3f2c1a7e-9b4d-4c8a-a1e2-5d6f7a8b9c0d";

    private static final List<JogoExterno> RESULTADO = List.of(new JogoExterno(
            "018d937f-590c-728b-ac35-38bcff85f086", "Elden Ring", null,
            List.of(new OfertaExterna(CodigoLoja.NUUVEM, "https://itad.link/elden", 24705, 27450L, (short) 10, null))));

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final ExecutorService executorDoServico = Executors.newVirtualThreadPerTaskExecutor();

    private final ListAppender<ILoggingEvent> logsDoServico = new ListAppender<>() {
        @Override
        protected void append(ILoggingEvent evento) {
            evento.prepareForDeferredProcessing();
            super.append(evento);
        }
    };

    @BeforeEach
    void capturarLogsDoServico() {
        logsDoServico.start();
        ((Logger) LoggerFactory.getLogger(BuscaExternaService.class)).addAppender(logsDoServico);
    }

    @AfterEach
    void encerrarExecutor() {
        executor.shutdownNow();
        executorDoServico.shutdownNow();
    }

    @AfterEach
    void pararDeCapturarLogsELimparMdc() {
        ((Logger) LoggerFactory.getLogger(BuscaExternaService.class)).detachAppender(logsDoServico);
        logsDoServico.stop();
        MDC.clear();
    }

    @Test
    void comRequestIdNoMdc_oLogDaChamadaAoItadSaiComORequestId_naThreadDaChamada() {
        MDC.put(REQUEST_ID, ID_DA_REQUISICAO);
        BuscaExternaService servico = servico(new PortaFalsa(), 20, CACHE_LONGO, PRAZO);

        assertThat(servico.buscar("elden ring")).isEqualTo(RESULTADO);

        assertThat(eventosQueComecamCom(CHAMADA_AO_ITAD)).singleElement().satisfies(evento -> {
            assertThat(evento.getThreadName()).isNotEqualTo(Thread.currentThread().getName());
            assertThat(evento.getMDCPropertyMap()).containsEntry(REQUEST_ID, ID_DA_REQUISICAO);
        });
        assertThat(MDC.get(REQUEST_ID)).isEqualTo(ID_DA_REQUISICAO);
    }

    @Test
    void semContextoNoMdc_aBuscaLogaSemContexto_eNaoLanca() {
        BuscaExternaService servico = servico(new PortaFalsa(), 20, CACHE_LONGO, PRAZO);

        assertThat(servico.buscar("elden ring")).isEqualTo(RESULTADO);

        assertThat(eventosQueComecamCom(CHAMADA_AO_ITAD)).singleElement()
                .satisfies(evento -> assertThat(evento.getMDCPropertyMap()).isEmpty());
    }

    @Test
    void prazoEstourado_oWarnDoPrazoSaiComORequestIdDaRequisicao() throws Exception {
        MDC.put(REQUEST_ID, ID_DA_REQUISICAO);
        PortaFalsa porta = new PortaFalsa();
        CountDownLatch soltar = porta.segurarPrimeiraChamada();
        BuscaExternaService servico = servico(porta, 20, CACHE_LONGO, Duration.ofMillis(200));

        try {
            assertThat(servico.buscar("elden ring")).isEmpty();
            aguardarEvento(AVISO_DE_PRAZO);
        } finally {
            soltar.countDown();
        }

        assertThat(eventosQueComecamCom(AVISO_DE_PRAZO)).singleElement().satisfies(evento -> {
            assertThat(evento.getLevel()).isEqualTo(Level.WARN);
            assertThat(evento.getThreadName()).isNotEqualTo(Thread.currentThread().getName());
            assertThat(evento.getMDCPropertyMap()).containsEntry(REQUEST_ID, ID_DA_REQUISICAO);
        });
        assertThat(MDC.get(REQUEST_ID)).isEqualTo(ID_DA_REQUISICAO);
    }

    @Test
    void tetoEsgotado_oAvisoRodaNaThreadDaRequisicao_eOMdcDelaContinuaComORequestId() {
        BuscaExternaService servico = servico(new PortaFalsa(), 1, CACHE_LONGO, PRAZO);
        servico.buscar("elden ring");
        MDC.put(REQUEST_ID, ID_DA_REQUISICAO);

        assertThat(servico.buscar("hollow knight")).isEmpty();

        assertThat(MDC.get(REQUEST_ID)).isEqualTo(ID_DA_REQUISICAO);
    }

    @Test
    void portaMaisLentaQueOPrazo_buscarVoltaVazioNoPrazo_semEsperarAPorta() {
        Duration prazo = Duration.ofMillis(200);
        PortaFalsa porta = new PortaFalsa();
        porta.atrasarPrimeiraChamada(prazo.multipliedBy(3));
        BuscaExternaService servico = servico(porta, 20, CACHE_LONGO, prazo);

        long inicio = System.nanoTime();
        List<JogoExterno> resultado = servico.buscar("elden ring");
        long esperaMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);

        assertThat(resultado).isEmpty();
        assertThat(esperaMs).isLessThanOrEqualTo(prazo.toMillis() + 200);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void chamadaQuePassaDoPrazoDoDono_registraUmWarnSo_mesmoComVariasRequisicoes(CapturedOutput saida)
            throws Exception {
        PortaFalsa porta = new PortaFalsa();
        CountDownLatch soltar = porta.segurarPrimeiraChamada();
        BuscaExternaService servico = servico(porta, 20, CACHE_LONGO, Duration.ofMillis(200));

        for (int i = 0; i < 4; i++) {
            assertThat(servico.buscar("elden ring")).isEmpty();
        }
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(LIMITE_DE_ESPERA_SEGUNDOS);
        while (!saida.getOut().contains(AVISO_DE_PRAZO) && System.nanoTime() < limite) {
            Thread.sleep(20);
        }
        Thread.sleep(300);
        soltar.countDown();

        assertThat(saida.getOut().split(Pattern.quote(AVISO_DE_PRAZO), -1)).hasSize(2);
        assertThat(saida.getOut()).contains(AVISO_DE_PRAZO + "elden ring");
        assertThat(porta.chamadas()).isEqualTo(1);
    }

    @Test
    void depoisQueAPortaLentaTermina_aBuscaSeguinteVemDoCache_semChamarAPortaDeNovo() throws Exception {
        Duration prazo = Duration.ofMillis(200);
        PortaFalsa porta = new PortaFalsa();
        porta.atrasarPrimeiraChamada(prazo.multipliedBy(3));
        BuscaExternaService servico = servico(porta, 20, CACHE_LONGO, prazo);

        assertThat(servico.buscar("elden ring")).isEmpty();
        porta.aguardarPrimeiraChamadaTerminar();

        assertThat(servico.buscar("elden ring")).isEqualTo(RESULTADO);
        assertThat(porta.chamadas()).isEqualTo(1);
    }

    @Test
    void segundaBuscaIgualDentroDoTtl_naoChamaAPortaENaoConsomeTeto() {
        PortaFalsa porta = new PortaFalsa();
        BuscaExternaService servico = servico(porta, 2, CACHE_LONGO, PRAZO);

        assertThat(servico.buscar("elden ring")).isEqualTo(RESULTADO);
        assertThat(servico.buscar("elden ring")).isEqualTo(RESULTADO);
        assertThat(servico.buscar("hollow knight")).isEqualTo(RESULTADO);

        assertThat(porta.termos()).containsExactly("elden ring", "hollow knight");
    }

    @Test
    void termosQueDiferemSoEmAcentoMaiusculaOuEspaco_saoAMesmaBusca() {
        PortaFalsa porta = new PortaFalsa();
        BuscaExternaService servico = servico(porta, 20, CACHE_LONGO, PRAZO);

        servico.buscar("Elden Ring");
        servico.buscar("elden ring");
        servico.buscar("eldén  ring");

        assertThat(porta.chamadas()).isEqualTo(1);
    }

    @Test
    void comTetoUm_dezBuscasIguaisConsomemUmaPermissao_eAProximaDeOutroTermoFicaSemExterno() throws Exception {
        PortaFalsa porta = new PortaFalsa();
        BuscaExternaService servico = servico(porta, 1, CACHE_LONGO, PRAZO);

        List<List<JogoExterno>> resultados = emParalelo(10, () -> servico.buscar("elden ring"));

        assertThat(resultados).hasSize(10).allSatisfy(resultado -> assertThat(resultado).isEqualTo(RESULTADO));
        assertThat(servico.buscar("hollow knight")).isEmpty();
        assertThat(porta.chamadas()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("falhasDaPorta")
    void falhaDaPorta_devolveVazio_eABuscaSeguinteTentaDeNovo(RuntimeException falha) {
        PortaFalsa porta = new PortaFalsa();
        porta.falharNaPrimeiraChamada(falha);
        BuscaExternaService servico = servico(porta, 20, CACHE_LONGO, PRAZO);

        assertThat(servico.buscar("elden ring")).isEmpty();
        assertThat(servico.buscar("elden ring")).isEqualTo(RESULTADO);
        assertThat(porta.chamadas()).isEqualTo(2);
    }

    static Stream<Arguments> falhasDaPorta() {
        return Stream.of(
                Arguments.of(new BuscaExternaIndisponivelException("ITAD fora do ar")),
                Arguments.of(new IllegalStateException("erro que ninguém previu")));
    }

    @Test
    void coalescencia_noveBuscasComODonoPreso_recebemOResultadoDeUmaChamadaSo() throws Exception {
        PortaFalsa porta = new PortaFalsa();
        CountDownLatch soltar = porta.segurarPrimeiraChamada();
        BuscaExternaService servico = servico(porta, 20, CACHE_LONGO, PRAZO);

        Future<List<JogoExterno>> dono = executor.submit(() -> servico.buscar("elden ring"));
        porta.aguardarPrimeiraChamada();

        List<Thread> esperando = new ArrayList<>();
        List<AtomicReference<List<JogoExterno>>> resultados = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            AtomicReference<List<JogoExterno>> resultado = new AtomicReference<>();
            resultados.add(resultado);
            esperando.add(Thread.ofPlatform().start(() -> resultado.set(servico.buscar("elden ring"))));
        }
        aguardarTodasBloqueadas(esperando);
        soltar.countDown();

        assertThat(dono.get(LIMITE_DE_ESPERA_SEGUNDOS, TimeUnit.SECONDS)).isEqualTo(RESULTADO);
        for (Thread thread : esperando) {
            thread.join(Duration.ofSeconds(LIMITE_DE_ESPERA_SEGUNDOS));
        }
        assertThat(resultados).allSatisfy(resultado -> assertThat(resultado.get()).isEqualTo(RESULTADO));
        assertThat(porta.chamadas()).isEqualTo(1);
    }

    @Test
    void quemEspera_desisteNoPrazo_eODonoPreencheOCacheAoTerminar() throws Exception {
        PortaFalsa porta = new PortaFalsa();
        porta.atrasarPrimeiraChamada(Duration.ofSeconds(1));
        BuscaExternaService servico = servico(porta, 20, CACHE_LONGO, Duration.ofMillis(200));

        Future<List<JogoExterno>> dono = executor.submit(() -> servico.buscar("elden ring"));
        porta.aguardarPrimeiraChamada();

        long inicio = System.nanoTime();
        List<JogoExterno> deQuemEspera = servico.buscar("elden ring");
        long esperaMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);

        assertThat(deQuemEspera).isEmpty();
        assertThat(esperaMs).isBetween(190L, 900L);
        assertThat(dono.get(LIMITE_DE_ESPERA_SEGUNDOS, TimeUnit.SECONDS)).isEmpty();
        porta.aguardarPrimeiraChamadaTerminar();
        assertThat(servico.buscar("elden ring")).isEqualTo(RESULTADO);
        assertThat(porta.chamadas()).isEqualTo(1);
    }

    @Test
    void donoQueNuncaCompleta_temOFuturoEncerradoPeloOrTimeout_eABuscaSeguinteChamaAPortaDeNovo()
            throws Exception {
        PortaFalsa porta = new PortaFalsa();
        CountDownLatch soltar = porta.segurarPrimeiraChamada();
        BuscaExternaService servico = servico(porta, 20, CACHE_LONGO, Duration.ofMillis(200));

        executor.submit(() -> servico.buscar("elden ring"));
        porta.aguardarPrimeiraChamada();

        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        List<JogoExterno> ultimo = List.of();
        while (porta.chamadas() < 2 && System.nanoTime() < limite) {
            ultimo = servico.buscar("elden ring");
        }
        soltar.countDown();

        assertThat(porta.chamadas()).isEqualTo(2);
        assertThat(ultimo).isEqualTo(RESULTADO);
    }

    @Test
    void quemEsperaInterrompido_recebeVazioEMantemAInterrupcao() throws Exception {
        PortaFalsa porta = new PortaFalsa();
        CountDownLatch soltar = porta.segurarPrimeiraChamada();
        BuscaExternaService servico = servico(porta, 20, CACHE_LONGO, PRAZO);

        executor.submit(() -> servico.buscar("elden ring"));
        porta.aguardarPrimeiraChamada();

        AtomicReference<List<JogoExterno>> resultado = new AtomicReference<>();
        AtomicBoolean interrompidoNaSaida = new AtomicBoolean();
        Thread quemEspera = Thread.ofPlatform().start(() -> {
            resultado.set(servico.buscar("elden ring"));
            interrompidoNaSaida.set(Thread.currentThread().isInterrupted());
        });
        aguardarTodasBloqueadas(List.of(quemEspera));
        quemEspera.interrupt();
        quemEspera.join(Duration.ofSeconds(LIMITE_DE_ESPERA_SEGUNDOS));
        soltar.countDown();

        assertThat(resultado.get()).isEmpty();
        assertThat(interrompidoNaSaida).isTrue();
    }

    @Test
    void construidoPelasPropriedades_deveUsarTtlEPrazoDelas() {
        PortaFalsa porta = new PortaFalsa();
        BuscaProperties propriedades = new BuscaProperties(new BuscaProperties.Externa(
                20, JANELA_LONGA, Duration.ofSeconds(1), Duration.ofSeconds(1), CACHE_LONGO, PRAZO, 5));
        BuscaExternaService servico = new BuscaExternaService(porta, new TetoDeBuscaExterna(propriedades), propriedades);

        assertThat(servico.buscar("elden ring")).isEqualTo(RESULTADO);
        assertThat(servico.buscar("elden ring")).isEqualTo(RESULTADO);
        assertThat(porta.chamadas()).isEqualTo(1);
    }

    private BuscaExternaService servico(BuscaExterna porta, int teto, Duration cacheTtl, Duration prazo) {
        return new BuscaExternaService(porta, new TetoDeBuscaExterna(teto, JANELA_LONGA), cacheTtl, prazo,
                executorDoServico);
    }

    private List<List<JogoExterno>> emParalelo(int quantidade, Supplier<List<JogoExterno>> busca) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<List<JogoExterno>>> futuros = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            futuros.add(executor.submit(() -> {
                largada.await();
                return busca.get();
            }));
        }
        largada.countDown();

        List<List<JogoExterno>> resultados = new ArrayList<>();
        for (Future<List<JogoExterno>> futuro : futuros) {
            resultados.add(futuro.get(LIMITE_DE_ESPERA_SEGUNDOS, TimeUnit.SECONDS));
        }
        return resultados;
    }

    private List<ILoggingEvent> eventosQueComecamCom(String prefixo) {
        synchronized (logsDoServico) {
            return logsDoServico.list.stream()
                    .filter(evento -> evento.getFormattedMessage().startsWith(prefixo))
                    .toList();
        }
    }

    private void aguardarEvento(String prefixo) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(LIMITE_DE_ESPERA_SEGUNDOS);
        while (eventosQueComecamCom(prefixo).isEmpty()) {
            assertThat(System.nanoTime()).as("evento de log não chegou: " + prefixo).isLessThan(limite);
            Thread.sleep(20);
        }
    }

    private static void aguardarTodasBloqueadas(List<Thread> threads) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(LIMITE_DE_ESPERA_SEGUNDOS);
        while (threads.stream().anyMatch(thread -> thread.getState() != Thread.State.TIMED_WAITING)) {
            assertThat(System.nanoTime()).as("threads não chegaram à espera do futuro").isLessThan(limite);
            Thread.sleep(5);
        }
    }

    private static final class PortaFalsa implements BuscaExterna {

        private final AtomicInteger chamadas = new AtomicInteger();
        private final List<String> termos = new CopyOnWriteArrayList<>();
        private final CountDownLatch primeiraChamadaEntrou = new CountDownLatch(1);
        private final CountDownLatch primeiraChamadaTerminou = new CountDownLatch(1);
        private volatile CountDownLatch travaDaPrimeiraChamada;
        private volatile Duration atrasoDaPrimeiraChamada;
        private volatile RuntimeException falhaDaPrimeiraChamada;

        @Override
        public List<JogoExterno> buscar(String termo) {
            int chamada = chamadas.incrementAndGet();
            termos.add(termo);
            primeiraChamadaEntrou.countDown();

            if (chamada == 1) {
                try {
                    segurar();
                    if (falhaDaPrimeiraChamada != null) {
                        throw falhaDaPrimeiraChamada;
                    }
                } finally {
                    primeiraChamadaTerminou.countDown();
                }
            }
            return RESULTADO;
        }

        void aguardarPrimeiraChamadaTerminar() throws InterruptedException {
            assertThat(primeiraChamadaTerminou.await(LIMITE_DE_ESPERA_SEGUNDOS, TimeUnit.SECONDS)).isTrue();
        }

        CountDownLatch segurarPrimeiraChamada() {
            travaDaPrimeiraChamada = new CountDownLatch(1);
            return travaDaPrimeiraChamada;
        }

        void atrasarPrimeiraChamada(Duration atraso) {
            atrasoDaPrimeiraChamada = atraso;
        }

        void falharNaPrimeiraChamada(RuntimeException falha) {
            falhaDaPrimeiraChamada = falha;
        }

        void aguardarPrimeiraChamada() throws InterruptedException {
            assertThat(primeiraChamadaEntrou.await(LIMITE_DE_ESPERA_SEGUNDOS, TimeUnit.SECONDS)).isTrue();
        }

        int chamadas() {
            return chamadas.get();
        }

        List<String> termos() {
            return termos;
        }

        private void segurar() {
            try {
                if (travaDaPrimeiraChamada != null) {
                    travaDaPrimeiraChamada.await(LIMITE_DE_ESPERA_SEGUNDOS, TimeUnit.SECONDS);
                }
                if (atrasoDaPrimeiraChamada != null) {
                    Thread.sleep(atrasoDaPrimeiraChamada);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
