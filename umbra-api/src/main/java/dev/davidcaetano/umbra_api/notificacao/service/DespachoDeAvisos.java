package dev.davidcaetano.umbra_api.notificacao.service;

import dev.davidcaetano.umbra_api.notificacao.DestinatarioEmEnvio;
import dev.davidcaetano.umbra_api.notificacao.LoteDoEmail;
import dev.davidcaetano.umbra_api.notificacao.SelecaoDoLote;
import dev.davidcaetano.umbra_api.notificacao.email.EmailService;
import dev.davidcaetano.umbra_api.notificacao.email.MensagemDeEmail;
import dev.davidcaetano.umbra_api.notificacao.email.RedacaoDoEmailDeAviso;
import dev.davidcaetano.umbra_api.notificacao.entity.EmailDeAvisoEntity;
import dev.davidcaetano.umbra_api.notificacao.mapper.DespachoDeAvisosMapper;
import dev.davidcaetano.umbra_api.notificacao.properties.EnvioDeAvisosProperties;
import dev.davidcaetano.umbra_api.notificacao.repository.AvisoDeAlvoRepository;
import dev.davidcaetano.umbra_api.notificacao.repository.EmailDeAvisoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "umbra.notificacao", name = "envio-habilitado", havingValue = "true")
public class DespachoDeAvisos {

    private final AvisoDeAlvoRepository avisoDeAlvoRepository;
    private final EmailDeAvisoRepository emailDeAvisoRepository;
    private final DespachoDeAvisosMapper mapper;
    private final SelecaoDoLote selecao;
    private final RedacaoDoEmailDeAviso redacao;
    private final EmailService emailService;
    private final TransactionTemplate transacao;
    private final EnvioDeAvisosProperties propriedades;
    private final Clock clock;
    private final ReentrantLock umDeCadaVez = new ReentrantLock();

    public DespachoDeAvisos(AvisoDeAlvoRepository avisoDeAlvoRepository,
                            EmailDeAvisoRepository emailDeAvisoRepository,
                            DespachoDeAvisosMapper mapper,
                            SelecaoDoLote selecao,
                            RedacaoDoEmailDeAviso redacao,
                            EmailService emailService,
                            TransactionTemplate transacao,
                            EnvioDeAvisosProperties propriedades,
                            Clock clock) {
        this.avisoDeAlvoRepository = avisoDeAlvoRepository;
        this.emailDeAvisoRepository = emailDeAvisoRepository;
        this.mapper = mapper;
        this.selecao = selecao;
        this.redacao = redacao;
        this.emailService = emailService;
        this.transacao = transacao;
        this.propriedades = propriedades;
        this.clock = clock;
    }

    @Scheduled(cron = "${umbra.notificacao.envio.agenda}", zone = "America/Sao_Paulo")
    public void despachar() {
        if (!umDeCadaVez.tryLock()) {
            log.warn("Despacho de avisos ignorado: o anterior ainda esta em andamento");
            return;
        }
        try {
            Instant agora = clock.instant();
            OffsetDateTime limite = emUtc(agora.minus(propriedades.validadeDoPendente()));
            Integer semDestino = transacao.execute(status -> avisoDeAlvoRepository.descartarSemDestino(limite));

            List<DestinatarioEmEnvio> destinatarios = mapper.agrupar(avisoDeAlvoRepository.listarPendentes(),
                    avisoDeAlvoRepository.listarOfertasDosPendentes(), agora);

            int enviados = 0;
            for (DestinatarioEmEnvio destinatario : destinatarios) {
                enviados += despacharComIsolamento(destinatario, agora) ? 1 : 0;
            }
            log.info("Despacho de avisos concluido: destinatarios={} emails={} descartadosSemDestino={}",
                    destinatarios.size(), enviados, semDestino);
        } finally {
            umDeCadaVez.unlock();
        }
    }

    private boolean despacharComIsolamento(DestinatarioEmEnvio destinatario, Instant agora) {
        try {
            return despacharPara(destinatario, agora);
        } catch (RuntimeException e) {
            log.warn("Despacho de avisos falhou para um destinatario: usuarioId={} causa={}",
                    destinatario.usuarioId(), e.getClass().getSimpleName(), e);
            return false;
        }
    }

    private boolean despacharPara(DestinatarioEmEnvio destinatario, Instant agora) {
        if (tetoDiarioAtingido(destinatario.usuarioId(), agora)) {
            log.info("Email de aviso adiado pelo teto diario: usuarioId={}", destinatario.usuarioId());
            return false;
        }

        LoteDoEmail lote = selecao.selecionar(destinatario.alvos());
        if (!lote.avisosDescartados().isEmpty()) {
            transacao.executeWithoutResult(status -> avisoDeAlvoRepository.descartar(lote.avisosDescartados()));
        }
        if (lote.vazio()) {
            return false;
        }

        MensagemDeEmail mensagem = redacao.redigir(lote, ZonedDateTime.ofInstant(agora, clock.getZone()));
        try {
            emailService.enviar(destinatario.email(), mensagem);
        } catch (MailException e) {
            transacao.executeWithoutResult(status ->
                    avisoDeAlvoRepository.registrarFalha(lote.avisosEnviados(), propriedades.tentativas()));
            log.warn("Email de aviso falhou: usuarioId={} itens={} causa={}",
                    destinatario.usuarioId(), lote.totalDeAlvos(), e.getClass().getSimpleName());
            return false;
        }

        OffsetDateTime enviadoEm = emUtc(agora);
        transacao.executeWithoutResult(status -> {
            avisoDeAlvoRepository.marcarEnviados(lote.avisosEnviados(), enviadoEm);
            emailDeAvisoRepository.save(EmailDeAvisoEntity.registrar(destinatario.usuarioId(), enviadoEm,
                    lote.totalDeAlvos()));
        });
        log.info("Email de aviso enviado: usuarioId={} itens={} restantes={}",
                destinatario.usuarioId(), lote.itens().size(), lote.restantes());
        return true;
    }

    private boolean tetoDiarioAtingido(Long usuarioId, Instant agora) {
        OffsetDateTime inicioDoDia = agora.atZone(clock.getZone()).toLocalDate().atStartOfDay(clock.getZone())
                .toOffsetDateTime();
        return emailDeAvisoRepository.countByUsuarioIdAndEnviadoEmGreaterThanEqual(usuarioId, inicioDoDia)
                >= propriedades.tetoDiarioPorUsuario();
    }

    private static OffsetDateTime emUtc(Instant instante) {
        return instante.atOffset(ZoneOffset.UTC);
    }
}
