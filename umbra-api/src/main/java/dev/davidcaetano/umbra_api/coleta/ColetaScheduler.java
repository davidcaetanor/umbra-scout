package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.coleta.itad.ItadRefresh;
import dev.davidcaetano.umbra_api.coleta.itad.ResolucaoDeGidFaltante;
import dev.davidcaetano.umbra_api.coleta.properties.AgendaColetaProperties;
import dev.davidcaetano.umbra_api.coleta.service.ColetaService;
import dev.davidcaetano.umbra_api.coleta.service.VereditoRodada;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

@Slf4j
@Component
public class ColetaScheduler {

    public static final String MDC_KEY = "rodadaId";

    private static final String CHAVE_DESCOBERTA = "umbra.coleta.agenda.descoberta";
    private static final String CHAVE_REFRESH = "umbra.coleta.agenda.refresh";
    private static final String CHAVE_RODADA_INICIAL = "umbra.coleta.agenda.rodada-inicial";
    private static final String VARIAVEL_RODADA_INICIAL = "UMBRA_COLETA_AGENDA_RODADAINICIAL";
    private static final String FALHA = "FALHA";

    private final List<Scraper> scrapers;
    private final ColetaService coletaService;
    private final ResolucaoDeGidFaltante resolucaoDeGidFaltante;
    private final ItadRefresh itadRefresh;
    private final Map<OrigemColeta, String> cronPorFonte;
    private final String cronRefresh;
    private final boolean rodadaInicial;
    private final Clock clock;
    private final Map<OrigemColeta, Semaphore> bloqueioPorFonte = new HashMap<>();
    private final Semaphore bloqueioDoRefresh = new Semaphore(1);
    private ScheduledTaskRegistrar registrar;

    public ColetaScheduler(List<Scraper> scrapers,
                           ColetaService coletaService,
                           ResolucaoDeGidFaltante resolucaoDeGidFaltante,
                           ItadRefresh itadRefresh,
                           AgendaColetaProperties agenda,
                           Clock clock) {

        validarAgenda(scrapers, agenda);

        this.scrapers = List.copyOf(scrapers);
        this.coletaService = coletaService;
        this.resolucaoDeGidFaltante = resolucaoDeGidFaltante;
        this.itadRefresh = itadRefresh;
        this.cronPorFonte = Map.copyOf(agenda.descoberta());
        this.cronRefresh = agenda.refresh();
        this.rodadaInicial = agenda.rodadaInicial();
        this.clock = clock;
        this.scrapers.forEach(scraper -> bloqueioPorFonte.put(scraper.fonte(), new Semaphore(1)));
    }

    public void executarDescoberta(Scraper scraper) {
        Semaphore bloqueio = Objects.requireNonNull(bloqueioPorFonte.get(scraper.fonte()),
                () -> "Scraper sem tarefa registrada na agenda: fonte=" + scraper.fonte());
        executar("tarefa=descoberta fonte=" + scraper.fonte(), bloqueio, () -> coletaService.gravar(scraper.coletar()));
    }

    public void executarRefresh() {
        executar("tarefa=refresh", bloqueioDoRefresh, () -> {
            resolverGidFaltante();
            return coletaService.gravar(itadRefresh.atualizar());
        });
    }

    public void registrarTarefas(ScheduledTaskRegistrar registrar) {
        ZoneId zona = clock.getZone();

        for (Scraper scraper : scrapers) {
            String cron = cronPorFonte.get(scraper.fonte());
            registrar.addTriggerTask(() -> executarDescoberta(scraper), new CronTrigger(cron, zona));
            log.info("Tarefa de coleta registrada: tarefa=descoberta fonte={} cron={} fuso={}", scraper.fonte(), cron, zona);
        }

        registrar.addTriggerTask(this::executarRefresh, new CronTrigger(cronRefresh, zona));
        log.info("Tarefa de coleta registrada: tarefa=refresh cron={} fuso={}", cronRefresh, zona);

        this.registrar = registrar;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoFicarPronta() {
        if (registrar == null) {
            log.info("Coleta agendada desabilitada: nenhuma tarefa registrada.");
            if (rodadaInicial) {
                log.warn("Rodada inicial pedida ({}=true), mas a coleta agendada esta desabilitada: nada vai rodar",
                        CHAVE_RODADA_INICIAL);
            }
            return;
        }

        if (!rodadaInicial) {
            log.info("Rodada inicial desligada. Para ligar, {}=true ({}=true)", CHAVE_RODADA_INICIAL, VARIAVEL_RODADA_INICIAL);
            return;
        }

        TaskScheduler scheduler = Objects.requireNonNull(registrar.getScheduler(),
                "ScheduledTaskRegistrar sem scheduler no ApplicationReadyEvent");
        log.info("Rodada inicial agendada: descobertas na ordem dos Scraper e o refresh por ultimo");
        scheduler.schedule(this::executarRodadaInicial, clock.instant());
    }

    private void executarRodadaInicial() {
        scrapers.forEach(this::executarDescoberta);
        executarRefresh();
    }

    private void executar(String tarefa, Semaphore bloqueio, Supplier<VereditoRodada> rodada) {
        if (!bloqueio.tryAcquire()) {
            log.warn("Rodada ignorada: {} ja em andamento", tarefa);
            return;
        }

        try {
            Instant inicio = clock.instant();
            MDC.put(MDC_KEY, UUID.randomUUID().toString());
            log.info("Rodada iniciada: {}", tarefa);
            String situacao = situacaoOuFalha(tarefa, rodada);
            log.info("Rodada finalizada: {} situacao={} duracaoMs={}",
                    tarefa, situacao, Duration.between(inicio, clock.instant()).toMillis());
        } finally {
            MDC.remove(MDC_KEY);
            bloqueio.release();
        }
    }

    private String situacaoOuFalha(String tarefa, Supplier<VereditoRodada> rodada) {
        try {
            return String.valueOf(rodada.get().situacao());
        } catch (RuntimeException falha) {
            log.error("Rodada falhou: {}", tarefa, falha);
            return FALHA;
        }
    }

    private void resolverGidFaltante() {
        try {
            resolucaoDeGidFaltante.resolver();
        } catch (RuntimeException falha) {
            log.warn("Resolucao de gid faltante falhou; o refresh segue com os jogos que ja tem chave do ITAD", falha);
        }
    }

    private static void validarAgenda(List<Scraper> scrapers, AgendaColetaProperties agenda) {
        Map<OrigemColeta, Scraper> scraperPorFonte = new HashMap<>();
        for (Scraper scraper : scrapers) {
            Scraper anterior = scraperPorFonte.putIfAbsent(scraper.fonte(), scraper);
            if (anterior != null) {
                throw new IllegalStateException("Dois Scraper com a mesma fonte " + scraper.fonte() + ": "
                        + anterior.getClass().getName() + " e " + scraper.getClass().getName());
            }
        }

        Map<OrigemColeta, String> cronPorFonte = agenda.descoberta();

        for (OrigemColeta fonte : scraperPorFonte.keySet()) {
            if (!cronPorFonte.containsKey(fonte)) {
                throw new IllegalStateException("Scraper da fonte " + fonte + " sem cron em " + CHAVE_DESCOBERTA);
            }
        }

        for (Map.Entry<OrigemColeta, String> entrada : cronPorFonte.entrySet()) {
            if (!scraperPorFonte.containsKey(entrada.getKey())) {
                throw new IllegalStateException("Cron sem Scraper correspondente em " + CHAVE_DESCOBERTA
                        + " para a fonte " + entrada.getKey());
            }
            validarCron(entrada.getValue(), CHAVE_DESCOBERTA + "." + entrada.getKey());
        }

        validarCron(agenda.refresh(), CHAVE_REFRESH);
    }

    private static void validarCron(String cron, String chave) {
        try {
            CronExpression.parse(cron);
        } catch (IllegalArgumentException invalido) {
            throw new IllegalStateException("Cron invalido em " + chave + ": " + cron, invalido);
        }
    }
}
