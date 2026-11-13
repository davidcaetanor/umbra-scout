package dev.davidcaetano.umbra_api.usuario.repository;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import dev.davidcaetano.umbra_api.usuario.entity.UsuarioEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UsuarioRepositoryTest extends IntegrationTestBase {

    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-11-13T12:00:00Z");

    private static final String INSERT = """
            INSERT INTO usuario (email, senha_hash, discord_id, nome_exibicao, cep, cidade, uf)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveGravarUsuarioComSenhaEEncontrarPeloEmail() {
        usuarioRepository.saveAndFlush(UsuarioEntity.comSenha("David@Exemplo.com", "hash", "David", AGORA));

        assertThat(usuarioRepository.existsByEmail("david@exemplo.com")).isTrue();
        assertThat(usuarioRepository.existsByEmail("outro@exemplo.com")).isFalse();
    }

    @Test
    void deveRecusarEmailDuplicado() {
        usuarioRepository.saveAndFlush(UsuarioEntity.comSenha("david@exemplo.com", "hash", "David", AGORA));

        UsuarioEntity duplicado = UsuarioEntity.comDiscord("DAVID@exemplo.com", "123", "Outro", AGORA);

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_usuario_email");
    }

    @Test
    void deveRecusarEmailComMaiusculaGravadoSemNormalizar() {
        assertThatThrownBy(() -> inserir("David@exemplo.com", "hash", null, null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_usuario_email");
    }

    @Test
    void deveRecusarUsuarioSemSenhaESemDiscord() {
        assertThatThrownBy(() -> inserir("david@exemplo.com", null, null, null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_usuario_credencial");
    }

    @Test
    void deveRecusarDiscordDuplicado() {
        inserir("a@exemplo.com", null, "123", null, null, null);

        assertThatThrownBy(() -> inserir("b@exemplo.com", null, "123", null, null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_usuario_discord");
    }

    @Test
    void deveRecusarCidadeSemCep() {
        assertThatThrownBy(() -> inserir("david@exemplo.com", "hash", null, null, "Sao Paulo", "SP"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_usuario_endereco");
    }

    @Test
    void deveRecusarCepForaDoFormato() {
        assertThatThrownBy(() -> inserir("david@exemplo.com", "hash", null, "01310-10", null, null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_usuario_cep");
    }

    @Test
    void deveRecusarUfMinuscula() {
        assertThatThrownBy(() -> inserir("david@exemplo.com", "hash", null, "01310100", "Sao Paulo", "sp"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_usuario_uf");
    }

    private void inserir(String email, String senhaHash, String discordId, String cep, String cidade, String uf) {
        jdbcTemplate.update(INSERT, email, senhaHash, discordId, "Nome", cep, cidade, uf);
    }
}
