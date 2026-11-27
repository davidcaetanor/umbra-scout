package dev.davidcaetano.umbra_api.notificacao.service;

import dev.davidcaetano.umbra_api.coleta.RodadaDeColetaConcluida;
import dev.davidcaetano.umbra_api.monitoramento.properties.MonitoramentoProperties;
import dev.davidcaetano.umbra_api.monitoramento.repository.MonitoramentoRepository;
import dev.davidcaetano.umbra_api.notificacao.AvaliadorDeAlvo;
import dev.davidcaetano.umbra_api.notificacao.Avisar;
import dev.davidcaetano.umbra_api.notificacao.DecisaoDeAlvo;
import dev.davidcaetano.umbra_api.notificacao.Manter;
import dev.davidcaetano.umbra_api.notificacao.MonitoramentoEmAvaliacao;
import dev.davidcaetano.umbra_api.notificacao.Rearmar;
import dev.davidcaetano.umbra_api.notificacao.entity.AvisoDeAlvoEntity;
import dev.davidcaetano.umbra_api.notificacao.mapper.AvaliacaoDeAlvoMapper;
import dev.davidcaetano.umbra_api.notificacao.repository.AvisoDeAlvoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "umbra.notificacao", name = "avaliacao-habilitada", havingValue = "true")
public class AvaliacaoDeAlvosService {

    private final MonitoramentoRepository monitoramentoRepository;
    private final AvisoDeAlvoRepository avisoDeAlvoRepository;
    private final AvaliacaoDeAlvoMapper mapper;
    private final AvaliadorDeAlvo avaliador;
    private final TransactionTemplate transacao;
    private final MonitoramentoProperties monitoramentoProperties;
    private final Clock clock;
    private final ReentrantLock umaDeCadaVez = new ReentrantLock();

    public AvaliacaoDeAlvosService(MonitoramentoRepository monitoramentoRepository,
                                   AvisoDeAlvoRepository avisoDeAlvoRepository,
                                   AvaliacaoDeAlvoMapper mapper,
                                   AvaliadorDeAlvo avaliador,
                                   TransactionTemplate transacao,
                                   MonitoramentoProperties monitoramentoProperties,
                                   Clock clock) {
        this.monitoramentoRepository = monitoramentoRepository;
        this.avisoDeAlvoRepository = avisoDeAlvoRepository;
        this.mapper = mapper;
        this.avaliador = avaliador;
        this.transacao = transacao;
        this.monitoramentoProperties = monitoramentoProperties;
        this.clock = clock;
    }

    @EventListener
    public void aoConcluirRodada(RodadaDeColetaConcluida rodada) {
        umaDeCadaVez.lock();
        try {
            Instant agora = clock.instant();
            List<MonitoramentoEmAvaliacao> monitoramentos = mapper.agrupar(
                    monitoramentoRepository.listarOfertasDosAtivos(monitoramentoProperties.diasParaDescontinuado()),
                    agora);

            int avisados = 0;
            int rearmados = 0;
            for (MonitoramentoEmAvaliacao monitoramento : monitoramentos) {
                DecisaoDeAlvo decisao = avaliador.avaliar(monitoramento.estado(), monitoramento.ofertas(), agora);
                switch (decisao) {
                    case Avisar aviso -> avisados += registrar(monitoramento, aviso, agora) ? 1 : 0;
                    case Rearmar rearmar -> rearmados += rearmar(monitoramento);
                    case Manter manter -> {
                    }
                }
            }
            log.info("Avaliacao de alvos concluida: {} avaliados={} avisados={} rearmados={}",
                    rodada.tarefa(), monitoramentos.size(), avisados, rearmados);
        } finally {
            umaDeCadaVez.unlock();
        }
    }

    private boolean registrar(MonitoramentoEmAvaliacao monitoramento, Avisar aviso, Instant agora) {
        OffsetDateTime registradoEm = agora.atOffset(ZoneOffset.UTC);
        OffsetDateTime ultimoAvisoLido = emUtc(monitoramento.estado().ultimoAvisoEm());

        boolean registrado = Boolean.TRUE.equals(transacao.execute(status -> {
            int alterados = monitoramentoRepository.registrarAviso(monitoramento.monitoramentoId(), registradoEm,
                    aviso.melhorPrecoCentavos(), ultimoAvisoLido);
            if (alterados == 0) {
                return false;
            }
            avisoDeAlvoRepository.save(AvisoDeAlvoEntity.registrar(monitoramento.monitoramentoId(), aviso, registradoEm));
            return true;
        }));

        if (registrado) {
            log.info("Aviso de alvo registrado: monitoramentoId={} motivo={} lojas={}",
                    monitoramento.monitoramentoId(), aviso.motivo(), aviso.cruzaram().size());
        }
        return registrado;
    }

    private int rearmar(MonitoramentoEmAvaliacao monitoramento) {
        Integer alterados = transacao.execute(status ->
                monitoramentoRepository.rearmarAviso(monitoramento.monitoramentoId()));
        return alterados == null ? 0 : alterados;
    }

    private static OffsetDateTime emUtc(Instant instante) {
        return instante == null ? null : instante.atOffset(ZoneOffset.UTC);
    }
}
