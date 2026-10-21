package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import dev.davidcaetano.umbra_api.config.AgendamentoColetaConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class ColetaSchedulerIntegracaoTest extends IntegrationTestBase {

    private static final long ESPERA_SEGUNDOS = 5;
    private static final Pattern RODADA_ID_NO_JSON = Pattern.compile("\"rodadaId\":\"[0-9a-f-]{36}\"");

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ThreadPoolTaskSchedulerBuilder schedulerBuilder;

    @Test
    void naoDeveAgendarNadaComAFlagDesligadaMasDeveValidarAAgendaReal() {
        assertThat(context.getBeansOfType(AgendamentoColetaConfig.class)).isEmpty();
        assertThat(context.getBean(ColetaScheduler.class)).isNotNull();
    }

    @Test
    void deveLevarORodadaIdParaOJsonDoLog(CapturedOutput saida) {
        Scraper semOfertas = new Scraper() {
            @Override
            public OrigemColeta fonte() {
                return OrigemColeta.STEAM_API;
            }

            @Override
            public ResultadoColeta coletar() {
                return new ResultadoColeta(OrigemColeta.STEAM_API, List.of(), 0, 0,
                        new Reconciliacao(null, 0, 0), Cobertura.amostra());
            }
        };

        context.getBean(ColetaScheduler.class).executarDescoberta(semOfertas);

        assertThat(saida.getOut().lines().filter(linha -> linha.contains("Rodada finalizada")))
                .singleElement()
                .satisfies(linha -> assertThat(linha)
                        .contains("situacao=REJEITADA")
                        .containsPattern(RODADA_ID_NO_JSON));
    }

    @Test
    void deveRodarDuasTarefasCoincidentesAoMesmoTempo() throws Exception {
        ThreadPoolTaskScheduler scheduler = schedulerBuilder.build();
        scheduler.initialize();
        try {
            CountDownLatch ambasEmExecucao = new CountDownLatch(2);
            AtomicInteger concluidasJuntas = new AtomicInteger();
            Runnable esperaAOutra = () -> {
                ambasEmExecucao.countDown();
                try {
                    if (ambasEmExecucao.await(ESPERA_SEGUNDOS, TimeUnit.SECONDS)) {
                        concluidasJuntas.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            Instant agora = Instant.now();
            List<ScheduledFuture<?>> tarefas = Stream.of(esperaAOutra, esperaAOutra)
                    .<ScheduledFuture<?>>map(tarefa -> scheduler.schedule(tarefa, agora))
                    .toList();
            for (ScheduledFuture<?> tarefa : tarefas) {
                tarefa.get(2 * ESPERA_SEGUNDOS, TimeUnit.SECONDS);
            }

            assertThat(concluidasJuntas).hasValue(2);
        } finally {
            scheduler.shutdown();
        }
    }
}
