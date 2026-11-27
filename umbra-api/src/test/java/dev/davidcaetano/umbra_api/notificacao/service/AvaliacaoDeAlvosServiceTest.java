package dev.davidcaetano.umbra_api.notificacao.service;

import dev.davidcaetano.umbra_api.coleta.RodadaDeColetaConcluida;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import dev.davidcaetano.umbra_api.monitoramento.repository.MonitoramentoRepository;
import dev.davidcaetano.umbra_api.notificacao.MotivoDoAviso;
import dev.davidcaetano.umbra_api.notificacao.entity.AvisoDeAlvoEntity;
import dev.davidcaetano.umbra_api.notificacao.entity.LojaDoAviso;
import dev.davidcaetano.umbra_api.notificacao.repository.AvisoDeAlvoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class AvaliacaoDeAlvosServiceTest extends IntegrationTestBase {

    private static final RodadaDeColetaConcluida RODADA = new RodadaDeColetaConcluida("tarefa=refresh");

    @Autowired
    private ApplicationEventPublisher eventos;

    @Autowired
    private AvisoDeAlvoRepository avisoDeAlvoRepository;

    @Autowired
    private MonitoramentoRepository monitoramentoRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveRegistrarOAvisoComTodasAsLojasQueCruzaramDepoisDaRodada(CapturedOutput saida) {
        long produto = produto();
        long steam = ofertaComPreco(produto, "STEAM", 4500);
        long gog = ofertaComPreco(produto, "GOG", 4800);
        ofertaComPreco(produto, "NUUVEM", 6000);
        long monitoramento = monitoramento(produto, 5000);

        eventos.publishEvent(RODADA);

        List<AvisoDeAlvoEntity> avisos = avisoDeAlvoRepository.findByMonitoramentoIdOrderByRegistradoEm(monitoramento);
        assertThat(avisos).singleElement().satisfies(aviso -> {
            assertThat(aviso.getMotivo()).isEqualTo(MotivoDoAviso.ENTRADA);
            assertThat(aviso.getMelhorPrecoCentavos()).isEqualTo(4500);
            assertThat(aviso.getLojas()).containsExactly(new LojaDoAviso(steam, 4500), new LojaDoAviso(gog, 4800));
        });
        assertThat(precoAvisado(monitoramento)).isEqualTo(4500L);
        assertThat(saida.getOut())
                .contains("Aviso de alvo registrado: monitoramentoId=" + monitoramento + " motivo=ENTRADA lojas=2")
                .containsPattern("Avaliacao de alvos concluida: tarefa=refresh avaliados=\\d+ avisados=\\d+ rearmados=\\d+");
    }

    @Test
    void naoDeveAvisarDuasVezesOMesmoPrecoEmRodadasSeguidas() {
        long produto = produto();
        ofertaComPreco(produto, "STEAM", 4500);
        long monitoramento = monitoramento(produto, 5000);

        eventos.publishEvent(RODADA);
        eventos.publishEvent(RODADA);

        assertThat(avisoDeAlvoRepository.findByMonitoramentoIdOrderByRegistradoEm(monitoramento)).hasSize(1);
    }

    @Test
    void deveRearmarQuandoOPrecoSobeAcimaDoAlvo() {
        long produto = produto();
        long steam = ofertaComPreco(produto, "STEAM", 4500);
        long monitoramento = monitoramento(produto, 5000);
        eventos.publishEvent(RODADA);

        preco(steam, 5500, Instant.now());
        eventos.publishEvent(RODADA);

        assertThat(precoAvisado(monitoramento)).isNull();
    }

    @Test
    void naoDeveAvaliarMonitoramentoPausado() {
        long produto = produto();
        ofertaComPreco(produto, "STEAM", 4500);
        long monitoramento = monitoramento(produto, 5000);
        jdbc.update("UPDATE monitoramento SET ativo = false WHERE id = ?", monitoramento);

        eventos.publishEvent(RODADA);

        assertThat(avisoDeAlvoRepository.findByMonitoramentoIdOrderByRegistradoEm(monitoramento)).isEmpty();
    }

    @Test
    void naoDeveAvisarComPrecoAConfirmar() {
        long produto = produto();
        long steam = ofertaComPreco(produto, "STEAM", 4500);
        jdbc.update("UPDATE oferta SET verificado_em = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(Duration.ofDays(2))), steam);
        long monitoramento = monitoramento(produto, 5000);

        eventos.publishEvent(RODADA);

        assertThat(avisoDeAlvoRepository.findByMonitoramentoIdOrderByRegistradoEm(monitoramento)).isEmpty();
    }

    @Test
    void registrarAviso_soGravaQuandoOUltimoAvisoAindaEOLido() {
        long monitoramento = monitoramento(produto(), 5000);
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        int primeiro = monitoramentoRepository.registrarAviso(monitoramento, agora, 4500, null);
        int concorrente = monitoramentoRepository.registrarAviso(monitoramento, agora.plusSeconds(1), 4400, null);

        assertThat(primeiro).isEqualTo(1);
        assertThat(concorrente).isZero();
        assertThat(precoAvisado(monitoramento)).isEqualTo(4500L);
    }

    private Long precoAvisado(long monitoramentoId) {
        return jdbc.queryForObject("SELECT preco_avisado_centavos FROM monitoramento WHERE id = ?",
                Long.class, monitoramentoId);
    }

    private long monitoramento(long produtoId, long alvo) {
        long usuario = jdbc.queryForObject("""
                INSERT INTO usuario (email, senha_hash, nome_exibicao)
                VALUES (?, 'hash', 'Pessoa')
                RETURNING id
                """, Long.class, UUID.randomUUID() + "@umbra.dev");
        return jdbc.queryForObject("""
                INSERT INTO monitoramento (usuario_id, produto_id, preco_alvo_centavos)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, usuario, produtoId, alvo);
    }

    private long produto() {
        return jdbc.queryForObject("INSERT INTO produto (tipo, nome, ativo) VALUES ('JOGO', ?, true) RETURNING id",
                Long.class, "t51 " + UUID.randomUUID());
    }

    private long ofertaComPreco(long produtoId, String codigoLoja, long valor) {
        String identificador = UUID.randomUUID().toString();
        long oferta = jdbc.queryForObject("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url, ativa)
                VALUES (?, (SELECT id FROM loja WHERE codigo = ?), ?, ?, true)
                RETURNING id
                """, Long.class, produtoId, codigoLoja, identificador, "https://loja.test/" + identificador);
        preco(oferta, valor, Instant.now().minus(Duration.ofMinutes(5)));
        return oferta;
    }

    private void preco(long ofertaId, long valor, Instant coletadoEm) {
        jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, valor_original_centavos, desconto_pct, disponivel,
                                   origem_coleta, coletado_em)
                VALUES (?, ?, ?, 0, true, 'ITAD_API', ?)
                """, ofertaId, valor, valor, Timestamp.from(coletadoEm));
    }
}
