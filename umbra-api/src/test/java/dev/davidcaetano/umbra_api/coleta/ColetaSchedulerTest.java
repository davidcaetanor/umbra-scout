package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.coleta.itad.ItadRefresh;
import dev.davidcaetano.umbra_api.coleta.itad.ResolucaoDeGidFaltante;
import dev.davidcaetano.umbra_api.coleta.properties.AgendaColetaProperties;
import dev.davidcaetano.umbra_api.coleta.service.ColetaService;
import dev.davidcaetano.umbra_api.coleta.service.VereditoRodada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.SimpleTriggerContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class ColetaSchedulerTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T13:00:00Z"), SAO_PAULO);
    private static final String CRON_ITAD = "0 0 0,6,12,18 * * *";
    private static final String CRON_STEAM = "0 15 0,6,12,18 * * *";
    private static final String CRON_KABUM = "0 45 3,15 * * *";
    private static final String CRON_REFRESH = "0 30 0,6,12,18 * * *";
    private static final String TAREFA_REFRESH = "refresh";

    private final ColetaService coletaService = mock(ColetaService.class);
    private final ResolucaoDeGidFaltante resolucaoDeGidFaltante = mock(ResolucaoDeGidFaltante.class);
    private final ItadRefresh itadRefresh = mock(ItadRefresh.class);

    private final ScraperFalso itad = new ScraperFalso(OrigemColeta.ITAD_API);
    private final ScraperFalso steam = new ScraperFalso(OrigemColeta.STEAM_API);
    private final ScraperFalso kabum = new ScraperFalso(OrigemColeta.KABUM_API);

    @BeforeEach
    void configurarColaboradores() {
        when(coletaService.gravar(any())).thenReturn(new VereditoRodada(Set.of()));
        when(itadRefresh.atualizar()).thenReturn(resultadoVazio(OrigemColeta.ITAD_API));
    }

    @Test
    void deveConstruirQuandoAgendaCoerenteComOsScrapers() {
        assertThatCode(() -> scheduler(List.of(itad, steam, kabum), agendaCompleta(), CRON_REFRESH))
                .doesNotThrowAnyException();
    }

    @Test
    void deveFalharQuandoScraperNaoTemCron() {
        Map<OrigemColeta, String> semKabum = Map.of(OrigemColeta.ITAD_API, CRON_ITAD, OrigemColeta.STEAM_API, CRON_STEAM);

        assertThatThrownBy(() -> scheduler(List.of(itad, steam, kabum), semKabum, CRON_REFRESH))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("KABUM_API")
                .hasMessageContaining("umbra.coleta.agenda.descoberta");
    }

    @Test
    void deveFalharQuandoCronNaoTemScraper() {
        Map<OrigemColeta, String> comOrfao = new HashMap<>(agendaCompleta());
        comOrfao.put(OrigemColeta.NUUVEM_API, CRON_ITAD);

        assertThatThrownBy(() -> scheduler(List.of(itad, steam, kabum), comOrfao, CRON_REFRESH))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NUUVEM_API")
                .hasMessageContaining("umbra.coleta.agenda.descoberta");
    }

    @Test
    void deveFalharQuandoDoisScrapersTemAMesmaFonte() {
        ScraperFalso outroItad = new ScraperFalso(OrigemColeta.ITAD_API);

        assertThatThrownBy(() -> scheduler(List.of(itad, outroItad, steam, kabum), agendaCompleta(), CRON_REFRESH))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ITAD_API");
    }

    @Test
    void deveFalharQuandoCronDaDescobertaForInvalido() {
        Map<OrigemColeta, String> comCronInvalido = new HashMap<>(agendaCompleta());
        comCronInvalido.put(OrigemColeta.STEAM_API, "0 0 25 * * *");

        assertThatThrownBy(() -> scheduler(List.of(itad, steam, kabum), comCronInvalido, CRON_REFRESH))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("umbra.coleta.agenda.descoberta.STEAM_API")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveFalharQuandoCronDoRefreshForInvalido() {
        assertThatThrownBy(() -> scheduler(List.of(itad, steam, kabum), agendaCompleta(), "a cada seis horas"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("umbra.coleta.agenda.refresh")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveSeguirParaOutraFonteQuandoUmScraperLancar(CapturedOutput saida) {
        ScraperFalso quebrado = new ScraperFalso(OrigemColeta.ITAD_API, new IllegalStateException("ITAD fora do ar"));
        ColetaScheduler scheduler = scheduler(List.of(quebrado, steam, kabum), agendaCompleta(), CRON_REFRESH);

        assertThatCode(() -> scheduler.executarDescoberta(quebrado)).doesNotThrowAnyException();
        scheduler.executarDescoberta(steam);

        verify(coletaService).gravar(steam.resultado);
        assertThat(saida.getOut())
                .contains("ERROR")
                .contains("Rodada falhou: tarefa=descoberta fonte=ITAD_API")
                .contains("ITAD fora do ar")
                .contains("Rodada finalizada: tarefa=descoberta fonte=ITAD_API situacao=FALHA duracaoMs=0")
                .contains("Rodada finalizada: tarefa=descoberta fonte=STEAM_API situacao=SAUDAVEL duracaoMs=0");
    }

    @Test
    void deveManterRodadaIdSoDuranteAExecucao() {
        ColetaScheduler scheduler = scheduler(List.of(itad, steam, kabum), agendaCompleta(), CRON_REFRESH);

        scheduler.executarDescoberta(itad);
        assertThat(MDC.get(ColetaScheduler.MDC_KEY)).isNull();

        scheduler.executarDescoberta(itad);
        assertThat(MDC.get(ColetaScheduler.MDC_KEY)).isNull();

        assertThat(itad.rodadaIdsVistos).hasSize(2).doesNotContainNull().doesNotHaveDuplicates();
    }

    @Test
    void deveLimparRodadaIdQuandoScraperLancar() {
        ScraperFalso quebrado = new ScraperFalso(OrigemColeta.ITAD_API, new IllegalStateException("ITAD fora do ar"));
        ColetaScheduler scheduler = scheduler(List.of(quebrado, steam, kabum), agendaCompleta(), CRON_REFRESH);

        scheduler.executarDescoberta(quebrado);

        assertThat(quebrado.rodadaIdsVistos).singleElement().isNotNull();
        assertThat(MDC.get(ColetaScheduler.MDC_KEY)).isNull();
    }

    @Test
    void deveResolverGidFaltanteAntesDoRefresh() {
        ColetaScheduler scheduler = scheduler(List.of(itad, steam, kabum), agendaCompleta(), CRON_REFRESH);

        scheduler.executarRefresh();

        InOrder ordem = inOrder(resolucaoDeGidFaltante, itadRefresh, coletaService);
        ordem.verify(resolucaoDeGidFaltante).resolver();
        ordem.verify(itadRefresh).atualizar();
        ordem.verify(coletaService).gravar(any());
    }

    @Test
    void deveSeguirComORefreshQuandoResolucaoDeGidLancar(CapturedOutput saida) {
        when(resolucaoDeGidFaltante.resolver()).thenThrow(new IllegalStateException("lookup do ITAD falhou"));
        ColetaScheduler scheduler = scheduler(List.of(itad, steam, kabum), agendaCompleta(), CRON_REFRESH);

        scheduler.executarRefresh();

        verify(itadRefresh).atualizar();
        verify(coletaService).gravar(any());
        assertThat(saida.getOut())
                .contains("WARN")
                .contains("lookup do ITAD falhou")
                .contains("Rodada finalizada: tarefa=refresh situacao=SAUDAVEL");
    }

    @Test
    void deveLogarCadaTarefaRegistradaENaoLogarDesabilitada(CapturedOutput saida) {
        ColetaScheduler scheduler = scheduler(List.of(itad, steam, kabum), agendaCompleta(), CRON_REFRESH);

        scheduler.registrarTarefas(new ScheduledTaskRegistrar());
        scheduler.aoFicarPronta();

        assertThat(saida.getOut())
                .contains("tarefa=descoberta fonte=ITAD_API cron=" + CRON_ITAD + " fuso=America/Sao_Paulo")
                .contains("tarefa=descoberta fonte=STEAM_API cron=" + CRON_STEAM + " fuso=America/Sao_Paulo")
                .contains("tarefa=descoberta fonte=KABUM_API cron=" + CRON_KABUM + " fuso=America/Sao_Paulo")
                .contains("tarefa=refresh cron=" + CRON_REFRESH + " fuso=America/Sao_Paulo")
                .doesNotContain("desabilitada");
    }

    @Test
    void deveLogarDesabilitadaQuandoNenhumaTarefaFoiRegistrada(CapturedOutput saida) {
        ColetaScheduler scheduler = scheduler(List.of(itad, steam, kabum), agendaCompleta(), CRON_REFRESH);

        scheduler.aoFicarPronta();

        assertThat(saida.getOut())
                .contains("Coleta agendada desabilitada");
    }

    @Test
    void deveRegistrarUmaTarefaPorFonteMaisORefreshNoFusoDeSaoPaulo() {
        ColetaScheduler scheduler = scheduler(List.of(itad, steam, kabum), agendaCompleta(), CRON_REFRESH);
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        scheduler.registrarTarefas(registrar);

        assertThat(registrar.getTriggerTaskList()).hasSize(4);

        Map<String, Instant> proximaExecucaoPorTarefa = new HashMap<>();
        for (TriggerTask tarefa : registrar.getTriggerTaskList()) {
            proximaExecucaoPorTarefa.put(quemExecuta(tarefa), proximaExecucaoComRelogioEmUtc(tarefa));
        }

        assertThat(proximaExecucaoPorTarefa).containsExactlyInAnyOrderEntriesOf(Map.of(
                "ITAD_API", Instant.parse("2026-10-21T15:00:00Z"),
                "STEAM_API", Instant.parse("2026-10-21T15:15:00Z"),
                "KABUM_API", Instant.parse("2026-10-21T18:45:00Z"),
                TAREFA_REFRESH, Instant.parse("2026-10-21T15:30:00Z")));
    }

    @Test
    void deveRodarAsDescobertasNaOrdemDaListaEORefreshPorUltimoNaRodadaInicial() {
        Scraper itadMock = scraperMock(OrigemColeta.ITAD_API);
        Scraper steamMock = scraperMock(OrigemColeta.STEAM_API);
        Scraper kabumMock = scraperMock(OrigemColeta.KABUM_API);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        ColetaScheduler scheduler = schedulerComRodadaInicial(List.of(itadMock, steamMock, kabumMock));

        scheduler.registrarTarefas(registrarCom(taskScheduler));
        scheduler.aoFicarPronta();
        rodadaInicialAgendada(taskScheduler).run();

        InOrder ordem = inOrder(itadMock, steamMock, kabumMock, resolucaoDeGidFaltante, itadRefresh, coletaService);
        ordem.verify(itadMock).coletar();
        ordem.verify(steamMock).coletar();
        ordem.verify(kabumMock).coletar();
        ordem.verify(resolucaoDeGidFaltante).resolver();
        ordem.verify(itadRefresh).atualizar();
        ordem.verify(coletaService).gravar(any());
    }

    @Test
    void deveSeguirComAFilaDaRodadaInicialQuandoUmaFonteLancar() {
        Scraper itadMock = scraperMock(OrigemColeta.ITAD_API);
        Scraper steamMock = scraperMock(OrigemColeta.STEAM_API);
        Scraper kabumMock = scraperMock(OrigemColeta.KABUM_API);
        when(steamMock.coletar()).thenThrow(new IllegalStateException("Steam fora do ar"));
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        ColetaScheduler scheduler = schedulerComRodadaInicial(List.of(itadMock, steamMock, kabumMock));

        scheduler.registrarTarefas(registrarCom(taskScheduler));
        scheduler.aoFicarPronta();
        rodadaInicialAgendada(taskScheduler).run();

        verify(kabumMock).coletar();
        verify(itadRefresh).atualizar();
    }

    @Test
    void naoDeveAgendarRodadaInicialQuandoDesligada(CapturedOutput saida) {
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        ColetaScheduler scheduler = scheduler(List.of(itad, steam, kabum), agendaCompleta(), CRON_REFRESH);

        scheduler.registrarTarefas(registrarCom(taskScheduler));
        scheduler.aoFicarPronta();

        verifyNoInteractions(taskScheduler);
        assertThat(saida.getOut())
                .contains("Rodada inicial desligada")
                .contains("umbra.coleta.agenda.rodada-inicial=true")
                .contains("UMBRA_COLETA_AGENDA_RODADAINICIAL=true");
    }

    @Test
    void deveAvisarQuandoRodadaInicialPedidaComColetaDesabilitada(CapturedOutput saida) {
        ColetaScheduler scheduler = schedulerComRodadaInicial(List.of(itad, steam, kabum));

        scheduler.aoFicarPronta();

        assertThat(itad.rodadaIdsVistos).isEmpty();
        verifyNoInteractions(coletaService, itadRefresh, resolucaoDeGidFaltante);
        assertThat(saida.getOut())
                .contains("Coleta agendada desabilitada")
                .contains("WARN")
                .contains("Rodada inicial pedida");
    }

    @Test
    void deveIgnorarAMesmaTarefaQuandoJaEmAndamento(CapturedOutput saida) {
        ColetaScheduler scheduler = scheduler(List.of(itad, steam, kabum), agendaCompleta(), CRON_REFRESH);
        itad.aoColetarUmaVez(() -> scheduler.executarDescoberta(itad));

        scheduler.executarDescoberta(itad);

        assertThat(itad.rodadaIdsVistos).hasSize(1);
        assertThat(saida.getOut()).contains("Rodada ignorada: tarefa=descoberta fonte=ITAD_API ja em andamento");
    }

    @Test
    void deveLiberarOBloqueioDepoisDeFalha() {
        ScraperFalso quebrado = new ScraperFalso(OrigemColeta.ITAD_API, new IllegalStateException("ITAD fora do ar"));
        ColetaScheduler scheduler = scheduler(List.of(quebrado, steam, kabum), agendaCompleta(), CRON_REFRESH);

        scheduler.executarDescoberta(quebrado);
        scheduler.executarDescoberta(quebrado);

        assertThat(quebrado.rodadaIdsVistos).hasSize(2);
    }

    @Test
    void deveBloquearPorTarefaENaoEntreFontes() {
        ColetaScheduler scheduler = scheduler(List.of(itad, steam, kabum), agendaCompleta(), CRON_REFRESH);
        itad.aoColetarUmaVez(() -> scheduler.executarDescoberta(steam));

        scheduler.executarDescoberta(itad);

        assertThat(steam.rodadaIdsVistos).hasSize(1);
        verify(coletaService).gravar(steam.resultado);
    }

    private String quemExecuta(TriggerTask tarefa) {
        List<ScraperFalso> falsos = List.of(itad, steam, kabum);
        List<Integer> chamadasAntes = falsos.stream().map(falso -> falso.rodadaIdsVistos.size()).toList();
        int refreshesAntes = mockingDetails(itadRefresh).getInvocations().size();

        tarefa.getRunnable().run();

        for (int i = 0; i < falsos.size(); i++) {
            if (falsos.get(i).rodadaIdsVistos.size() > chamadasAntes.get(i)) {
                return String.valueOf(falsos.get(i).fonte());
            }
        }
        assertThat(mockingDetails(itadRefresh).getInvocations()).hasSizeGreaterThan(refreshesAntes);
        return TAREFA_REFRESH;
    }

    private static Instant proximaExecucaoComRelogioEmUtc(TriggerTask tarefa) {
        Clock relogioDoContainer = Clock.fixed(CLOCK.instant(), ZoneOffset.UTC);
        return tarefa.getTrigger().nextExecution(new SimpleTriggerContext(relogioDoContainer));
    }

    private ColetaScheduler scheduler(List<Scraper> scrapers, Map<OrigemColeta, String> descoberta, String refresh) {
        return new ColetaScheduler(scrapers, coletaService, resolucaoDeGidFaltante, itadRefresh,
                new AgendaColetaProperties(descoberta, refresh, false), CLOCK);
    }

    private ColetaScheduler schedulerComRodadaInicial(List<Scraper> scrapers) {
        return new ColetaScheduler(scrapers, coletaService, resolucaoDeGidFaltante, itadRefresh,
                new AgendaColetaProperties(agendaCompleta(), CRON_REFRESH, true), CLOCK);
    }

    private static ScheduledTaskRegistrar registrarCom(TaskScheduler taskScheduler) {
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        registrar.setTaskScheduler(taskScheduler);
        return registrar;
    }

    private static Scraper scraperMock(OrigemColeta fonte) {
        Scraper scraper = mock(Scraper.class);
        when(scraper.fonte()).thenReturn(fonte);
        when(scraper.coletar()).thenReturn(resultadoVazio(fonte));
        return scraper;
    }

    private static Runnable rodadaInicialAgendada(TaskScheduler taskScheduler) {
        ArgumentCaptor<Runnable> rodada = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(rodada.capture(), any(Instant.class));
        return rodada.getValue();
    }

    private static Map<OrigemColeta, String> agendaCompleta() {
        return Map.of(OrigemColeta.ITAD_API, CRON_ITAD,
                OrigemColeta.STEAM_API, CRON_STEAM,
                OrigemColeta.KABUM_API, CRON_KABUM);
    }

    private static ResultadoColeta resultadoVazio(OrigemColeta fonte) {
        return new ResultadoColeta(fonte, List.of(), 0, 0, new Reconciliacao(null, 0, 0), Cobertura.amostra());
    }

    private static final class ScraperFalso implements Scraper {

        private final OrigemColeta fonte;
        private final RuntimeException falha;
        private final ResultadoColeta resultado;
        private final List<String> rodadaIdsVistos = new ArrayList<>();
        private Runnable durante = () -> {
        };

        private ScraperFalso(OrigemColeta fonte) {
            this(fonte, null);
        }

        private ScraperFalso(OrigemColeta fonte, RuntimeException falha) {
            this.fonte = fonte;
            this.falha = falha;
            this.resultado = resultadoVazio(fonte);
        }

        @Override
        public OrigemColeta fonte() {
            return fonte;
        }

        private void aoColetarUmaVez(Runnable acao) {
            durante = acao;
        }

        @Override
        public ResultadoColeta coletar() {
            rodadaIdsVistos.add(MDC.get(ColetaScheduler.MDC_KEY));
            Runnable acao = durante;
            durante = () -> {
            };
            acao.run();
            if (falha != null) {
                throw falha;
            }
            return resultado;
        }
    }
}
