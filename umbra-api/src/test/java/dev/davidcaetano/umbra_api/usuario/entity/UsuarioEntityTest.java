package dev.davidcaetano.umbra_api.usuario.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioEntityTest {

    private static final String EMAIL = "usuario_xanisvaldo@exemplo.com";
    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-11-13T12:00:00Z");

    @Test
    void comSenhaDeveNormalizarEmailParaMinusculoSemEspacos() {
        UsuarioEntity usuario = UsuarioEntity.comSenha("  Usuario_Xanisvaldo@Exemplo.COM ", "hash", "Xanisvaldo", AGORA);

        assertThat(usuario.getEmail()).isEqualTo(EMAIL);
        assertThat(usuario.getSenhaHash()).isEqualTo("hash");
        assertThat(usuario.getDiscordId()).isNull();
        assertThat(usuario.getNomeExibicao()).isEqualTo("Xanisvaldo");
        assertThat(usuario.isAtivo()).isTrue();
        assertThat(usuario.getCriadoEm()).isEqualTo(AGORA);
        assertThat(usuario.getAtualizadoEm()).isEqualTo(AGORA);
    }

    @Test
    void comDiscordDeveCriarSemSenha() {
        UsuarioEntity usuario = UsuarioEntity.comDiscord(EMAIL, "123456789", "Xanisvaldo", AGORA);

        assertThat(usuario.getDiscordId()).isEqualTo("123456789");
        assertThat(usuario.getSenhaHash()).isNull();
        assertThat(usuario.getGoogleId()).isNull();
    }

    @Test
    void comGoogleDeveCriarSemSenhaESemDiscord() {
        UsuarioEntity usuario = UsuarioEntity.comGoogle(EMAIL, "108234567890", "Xanisvaldo", AGORA);

        assertThat(usuario.getGoogleId()).isEqualTo("108234567890");
        assertThat(usuario.getSenhaHash()).isNull();
        assertThat(usuario.getDiscordId()).isNull();
    }

    @Test
    void deveRecusarCredencialAusente() {
        assertThatThrownBy(() -> UsuarioEntity.comSenha(EMAIL, null, "Xanisvaldo", AGORA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("senhaHash");
        assertThatThrownBy(() -> UsuarioEntity.comDiscord(EMAIL, " ", "Xanisvaldo", AGORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("discordId");
        assertThatThrownBy(() -> UsuarioEntity.comGoogle(EMAIL, null, "Xanisvaldo", AGORA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("googleId");
    }

    @Test
    void deveRecusarNomeExibicaoVazio() {
        assertThatThrownBy(() -> UsuarioEntity.comSenha(EMAIL, "hash", "  ", AGORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nomeExibicao");
    }

    @Test
    void deveRecusarEmailAusente() {
        assertThatThrownBy(() -> UsuarioEntity.comSenha(null, "hash", "Xanisvaldo", AGORA))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("email");
    }
}
