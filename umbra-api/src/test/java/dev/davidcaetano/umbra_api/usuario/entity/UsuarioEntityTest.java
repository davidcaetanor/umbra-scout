package dev.davidcaetano.umbra_api.usuario.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioEntityTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-11-13T12:00:00Z");

    @Test
    void comSenhaDeveNormalizarEmailParaMinusculoSemEspacos() {
        UsuarioEntity usuario = UsuarioEntity.comSenha("  David@Exemplo.COM ", "hash", "David", AGORA);

        assertThat(usuario.getEmail()).isEqualTo("david@exemplo.com");
        assertThat(usuario.getSenhaHash()).isEqualTo("hash");
        assertThat(usuario.getDiscordId()).isNull();
        assertThat(usuario.getNomeExibicao()).isEqualTo("David");
        assertThat(usuario.isAtivo()).isTrue();
        assertThat(usuario.getCriadoEm()).isEqualTo(AGORA);
        assertThat(usuario.getAtualizadoEm()).isEqualTo(AGORA);
    }

    @Test
    void comDiscordDeveCriarSemSenha() {
        UsuarioEntity usuario = UsuarioEntity.comDiscord("david@exemplo.com", "123456789", "David", AGORA);

        assertThat(usuario.getDiscordId()).isEqualTo("123456789");
        assertThat(usuario.getSenhaHash()).isNull();
    }

    @Test
    void deveRecusarCredencialAusente() {
        assertThatThrownBy(() -> UsuarioEntity.comSenha("david@exemplo.com", null, "David", AGORA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("senhaHash");
        assertThatThrownBy(() -> UsuarioEntity.comDiscord("david@exemplo.com", " ", "David", AGORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("discordId");
    }

    @Test
    void deveRecusarNomeExibicaoVazio() {
        assertThatThrownBy(() -> UsuarioEntity.comSenha("david@exemplo.com", "hash", "  ", AGORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nomeExibicao");
    }

    @Test
    void deveRecusarEmailAusente() {
        assertThatThrownBy(() -> UsuarioEntity.comSenha(null, "hash", "David", AGORA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("email");
    }
}
