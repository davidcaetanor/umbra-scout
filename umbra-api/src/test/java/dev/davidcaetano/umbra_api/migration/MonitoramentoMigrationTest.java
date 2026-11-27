package dev.davidcaetano.umbra_api.migration;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class MonitoramentoMigrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private Long inserirUsuario(String email) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (email, senha_hash, nome_exibicao)
                VALUES (?, 'hash', 'Pessoa')
                RETURNING id
                """, Long.class, email);
    }

    private Long inserirProduto() {
        return jdbc.queryForObject(
                "INSERT INTO produto (tipo, nome) VALUES ('JOGO', 'Hollow Knight') RETURNING id", Long.class);
    }

    private Long inserirMonitoramento(Long usuarioId, Long produtoId, long alvo) {
        return jdbc.queryForObject("""
                INSERT INTO monitoramento (usuario_id, produto_id, preco_alvo_centavos)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, usuarioId, produtoId, alvo);
    }

    private int monitoramentosDoUsuario(Long usuarioId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM monitoramento WHERE usuario_id = ?", Integer.class, usuarioId);
    }

    @Test
    void deveApagarOsMonitoramentosQuandoAContaForApagada() {
        Long usuarioId = inserirUsuario("lgpd@umbra.dev");
        inserirMonitoramento(usuarioId, inserirProduto(), 5000);
        inserirMonitoramento(usuarioId, inserirProduto(), 7000);

        jdbc.update("DELETE FROM usuario WHERE id = ?", usuarioId);

        assertThat(monitoramentosDoUsuario(usuarioId)).isZero();
    }

    @Test
    void deveManterOsMonitoramentosDeOutroUsuarioQuandoUmaContaForApagada() {
        Long apagado = inserirUsuario("sai@umbra.dev");
        Long mantido = inserirUsuario("fica@umbra.dev");
        Long produtoId = inserirProduto();
        inserirMonitoramento(apagado, produtoId, 5000);
        inserirMonitoramento(mantido, produtoId, 5000);

        jdbc.update("DELETE FROM usuario WHERE id = ?", apagado);

        assertThat(monitoramentosDoUsuario(mantido)).isEqualTo(1);
    }

    @Test
    void deveRecusarApagarProdutoMonitorado() {
        Long produtoId = inserirProduto();
        inserirMonitoramento(inserirUsuario("restrict@umbra.dev"), produtoId, 5000);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM produto WHERE id = ?", produtoId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_monit_produto");
    }

    @Test
    void deveRecusarOMesmoProdutoMonitoradoDuasVezesPeloMesmoUsuario() {
        Long usuarioId = inserirUsuario("duplo@umbra.dev");
        Long produtoId = inserirProduto();
        inserirMonitoramento(usuarioId, produtoId, 5000);

        assertThatThrownBy(() -> inserirMonitoramento(usuarioId, produtoId, 4000))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_monit_usuario_produto");
    }

    @Test
    void deveRecusarAlvoZero() {
        Long usuarioId = inserirUsuario("zero@umbra.dev");
        Long produtoId = inserirProduto();

        assertThatThrownBy(() -> inserirMonitoramento(usuarioId, produtoId, 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_monit_alvo");
    }

    @Test
    void deveRecusarMonitoramentoDeUsuarioInexistente() {
        Long produtoId = inserirProduto();

        assertThatThrownBy(() -> inserirMonitoramento(Long.MAX_VALUE, produtoId, 5000))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_monit_usuario");
    }

    @Test
    void deveNascerAtivoESemAviso() {
        Long id = inserirMonitoramento(inserirUsuario("padrao@umbra.dev"), inserirProduto(), 5000);

        assertThat(jdbc.queryForObject("SELECT ativo FROM monitoramento WHERE id = ?", Boolean.class, id)).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT ultimo_aviso_em IS NULL FROM monitoramento WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    void deveNascerSemPrecoAvisado() {
        Long id = inserirMonitoramento(inserirUsuario("avisado@umbra.dev"), inserirProduto(), 5000);

        assertThat(jdbc.queryForObject(
                "SELECT preco_avisado_centavos IS NULL FROM monitoramento WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    void deveApagarOsAvisosQuandoOMonitoramentoForApagado() {
        Long monitoramentoId = inserirMonitoramento(inserirUsuario("aviso@umbra.dev"), inserirProduto(), 5000);
        inserirAviso(monitoramentoId, "ENTRADA");

        jdbc.update("DELETE FROM monitoramento WHERE id = ?", monitoramentoId);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM aviso_de_alvo WHERE monitoramento_id = ?",
                Integer.class, monitoramentoId)).isZero();
    }

    @Test
    void deveRecusarMotivoDeAvisoDesconhecido() {
        Long monitoramentoId = inserirMonitoramento(inserirUsuario("motivo@umbra.dev"), inserirProduto(), 5000);

        assertThatThrownBy(() -> inserirAviso(monitoramentoId, "SUBIDA"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_aviso_motivo");
    }

    private void inserirAviso(Long monitoramentoId, String motivo) {
        jdbc.update("""
                INSERT INTO aviso_de_alvo (monitoramento_id, motivo, melhor_preco_centavos, registrado_em)
                VALUES (?, ?, 4500, now())
                """, monitoramentoId, motivo);
    }
}
