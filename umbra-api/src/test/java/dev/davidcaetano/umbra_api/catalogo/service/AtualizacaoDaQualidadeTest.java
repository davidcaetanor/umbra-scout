package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.properties.QualidadeProperties;
import dev.davidcaetano.umbra_api.catalogo.repository.QualidadeJogoRepository;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class AtualizacaoDaQualidadeTest extends IntegrationTestBase {

    private static final Instant AGORA = Instant.parse("2026-11-27T12:00:00Z");
    private static final Duration VALIDADE = Duration.ofDays(7);
    private static final QualidadeDoJogo HOLLOW_KNIGHT = new QualidadeDoJogo(96, 489363, 89, 76, 19727, "Team Cherry");

    @Autowired
    private QualidadeJogoRepository qualidadeJogoRepository;

    @Autowired
    private AtualizacaoDaVitrine atualizacaoDaVitrine;

    @Autowired
    private JdbcTemplate jdbc;

    private final FonteFalsa fonte = new FonteFalsa();

    @Test
    void deveGravarAQualidadeDevolvidaPelaFonte() {
        long jogo = jogo("t47q Hollow Knight");
        fonte.responder(chaveDe(jogo), HOLLOW_KNIGHT);

        atualizacao(500).atualizarLote();

        assertThat(qualidadeDe(jogo))
                .containsEntry("steam_nota", 96)
                .containsEntry("steam_avaliacoes", 489363)
                .containsEntry("critica_nota", 89)
                .containsEntry("critica_avaliacoes", 76)
                .containsEntry("lista_desejos", 19727)
                .containsEntry("publicador", "Team Cherry")
                .containsEntry("consultada_em", Timestamp.from(AGORA));
    }

    @Test
    void jogoQueAFonteNaoConhece_deveFicarConsultadoSemAvaliacao() {
        long jogo = jogo("t47q Desconhecido");

        atualizacao(500).atualizarLote();

        assertThat(qualidadeDe(jogo))
                .containsEntry("steam_nota", null)
                .containsEntry("critica_nota", null)
                .containsEntry("consultada_em", Timestamp.from(AGORA));
    }

    @Test
    void emPromocaoENuncaConsultado_vemAntesDoResto() {
        long semPromocao = jogo("t47q Sem promocao");
        preco(semPromocao, 5000, 5000);
        long jaConsultadoEmPromocao = jogo("t47q Ja consultado");
        preco(jaConsultadoEmPromocao, 2000, 10000);
        consultadoEm(jaConsultadoEmPromocao, AGORA.minus(Duration.ofDays(8)));
        long emPromocao = jogo("t47q Em promocao");
        preco(emPromocao, 2000, 10000);
        atualizacaoDaVitrine.atualizar();

        atualizacao(500).atualizarLote();

        assertThat(fonte.consultadas)
                .containsSubsequence(chaveDe(emPromocao), chaveDe(jaConsultadoEmPromocao), chaveDe(semPromocao));
    }

    @Test
    void consultadoDentroDaValidade_naoVoltaAFila() {
        long recente = jogo("t47q Recente");
        consultadoEm(recente, AGORA.minus(Duration.ofDays(6)));
        long vencido = jogo("t47q Vencido");
        consultadoEm(vencido, AGORA.minus(Duration.ofDays(8)));

        atualizacao(500).atualizarLote();

        assertThat(fonte.consultadas).contains(chaveDe(vencido)).doesNotContain(chaveDe(recente));
    }

    @Test
    void hardwareInativoESemChave_ficamForaDaFila() {
        long hardware = produto("t47q Placa", "HARDWARE", true, UUID.randomUUID().toString());
        long inativo = produto("t47q Inativo", "JOGO", false, UUID.randomUUID().toString());
        produto("t47q Sem chave", "JOGO", true, null);

        atualizacao(500).atualizarLote();

        assertThat(fonte.consultadas).doesNotContain(chaveDe(hardware), chaveDe(inativo));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM qualidade_jogo q JOIN produto p ON p.id = q.produto_id "
                + "WHERE p.nome LIKE 't47q %' AND (p.tipo = 'HARDWARE' OR NOT p.ativo OR p.chave_itad IS NULL)",
                Long.class)).isZero();
    }

    @Test
    void lote_deveLimitarAsConsultasDaExecucao() {
        jogo("t47q Lote A");
        jogo("t47q Lote B");
        jogo("t47q Lote C");

        atualizacao(2).atualizarLote();

        assertThat(fonte.consultadas).hasSize(2);
    }

    @Test
    void falhaNaFonte_interrompeOLoteEGuardaOQueJaFoiConsultado(CapturedOutput saida) {
        long primeiro = jogo("t47q Primeiro");
        preco(primeiro, 1000, 9000);
        long segundo = jogo("t47q Segundo");
        preco(segundo, 1000, 5000);
        atualizacaoDaVitrine.atualizar();
        fonte.falharEm(chaveDe(segundo));

        atualizacao(500).atualizarLote();

        assertThat(qualidadeDe(primeiro)).containsEntry("consultada_em", Timestamp.from(AGORA));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM qualidade_jogo WHERE produto_id = ?", Long.class, segundo))
                .isZero();
        assertThat(saida.getOut()).contains("Atualizacao da qualidade interrompida").contains("ITAD fora do ar");
    }

    private AtualizacaoDaQualidade atualizacao(int lote) {
        return new AtualizacaoDaQualidade(qualidadeJogoRepository, fonte, new QualidadeProperties(lote, VALIDADE),
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private Map<String, Object> qualidadeDe(long produtoId) {
        return jdbc.queryForMap("SELECT * FROM qualidade_jogo WHERE produto_id = ?", produtoId);
    }

    private long jogo(String nome) {
        return produto(nome, "JOGO", true, UUID.randomUUID().toString());
    }

    private long produto(String nome, String tipo, boolean ativo, String chaveItad) {
        return jdbc.queryForObject("""
                INSERT INTO produto (tipo, nome, ativo, chave_itad) VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, tipo, nome, ativo, chaveItad);
    }

    private String chaveDe(long produtoId) {
        return jdbc.queryForObject("SELECT chave_itad FROM produto WHERE id = ?", String.class, produtoId);
    }

    private void consultadoEm(long produtoId, Instant quando) {
        jdbc.update("INSERT INTO qualidade_jogo (produto_id, consultada_em) VALUES (?, ?)", produtoId,
                Timestamp.from(quando));
    }

    private void preco(long produtoId, long valor, long original) {
        String identificador = UUID.randomUUID().toString();
        Long ofertaId = jdbc.queryForObject("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url, ativa)
                VALUES (?, (SELECT id FROM loja WHERE codigo = 'STEAM'), ?, ?, true)
                RETURNING id
                """, Long.class, produtoId, identificador, "https://loja.test/" + identificador);
        jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, valor_original_centavos, desconto_pct, disponivel,
                                   origem_coleta, coletado_em)
                VALUES (?, ?, ?, 0, true, 'ITAD_API', now() - INTERVAL '1 hour')
                """, ofertaId, valor, original);
    }

    private static final class FonteFalsa implements FonteDeQualidade {

        private final Map<String, QualidadeDoJogo> respostas = new HashMap<>();
        private final List<String> consultadas = new ArrayList<>();
        private String chaveQueFalha;

        void responder(String chave, QualidadeDoJogo qualidade) {
            respostas.put(chave, qualidade);
        }

        void falharEm(String chave) {
            chaveQueFalha = chave;
        }

        @Override
        public Optional<QualidadeDoJogo> consultar(String chaveItad) {
            if (chaveItad.equals(chaveQueFalha)) {
                throw new IllegalStateException("ITAD fora do ar");
            }
            consultadas.add(chaveItad);
            return Optional.ofNullable(respostas.get(chaveItad));
        }
    }
}
