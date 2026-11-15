package dev.davidcaetano.umbra_api.usuario.oauth2;

import dev.davidcaetano.umbra_api.usuario.service.IdentidadeExterna;
import dev.davidcaetano.umbra_api.usuario.service.ProvedorDeIdentidade;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExtratorDeIdentidadeTest {

    private final DiscordExtratorDeIdentidade discord = new DiscordExtratorDeIdentidade();
    private final GoogleExtratorDeIdentidade google = new GoogleExtratorDeIdentidade();

    @Test
    void discord_deveLerIdEmailVerificacaoENomeGlobal() {
        IdentidadeExterna identidade = discord.extrair(usuario("id", Map.of("id", "123", "username", "apelido",
                "global_name", "Nome Global", "email", "a@exemplo.com", "verified", true)));

        assertThat(identidade).isEqualTo(new IdentidadeExterna(ProvedorDeIdentidade.DISCORD, "123", "a@exemplo.com",
                true, "Nome Global"));
    }

    @Test
    void discordSemNomeGlobal_deveUsarOUsername_eSemVerified_naoDeveConsiderarVerificado() {
        Map<String, Object> atributos = new HashMap<>(Map.of("id", "123", "username", "apelido"));
        atributos.put("global_name", null);

        IdentidadeExterna identidade = discord.extrair(usuario("id", atributos));

        assertThat(identidade.nome()).isEqualTo("apelido");
        assertThat(identidade.emailVerificado()).isFalse();
        assertThat(identidade.email()).isNull();
    }

    @Test
    void google_deveLerSubEmailVerificadoENome() {
        IdentidadeExterna identidade = google.extrair(usuario("sub", Map.of("sub", "999", "email", "g@exemplo.com",
                "email_verified", true, "name", "Nome Google")));

        assertThat(identidade).isEqualTo(new IdentidadeExterna(ProvedorDeIdentidade.GOOGLE, "999", "g@exemplo.com",
                true, "Nome Google"));
    }

    @Test
    void googleComEmailNaoVerificado_naoDeveConsiderarVerificado() {
        IdentidadeExterna identidade = google.extrair(usuario("sub", Map.of("sub", "999", "email", "g@exemplo.com",
                "email_verified", false)));

        assertThat(identidade.emailVerificado()).isFalse();
    }

    @Test
    void toString_naoDeveExporEmailNemNome() {
        IdentidadeExterna identidade = new IdentidadeExterna(ProvedorDeIdentidade.GOOGLE, "999", "g@exemplo.com",
                true, "Nome Google");

        assertThat(identidade.toString()).doesNotContain("g@exemplo.com", "Nome Google");
    }

    private static DefaultOAuth2User usuario(String atributoDeNome, Map<String, Object> atributos) {
        return new DefaultOAuth2User(List.of(), atributos, atributoDeNome);
    }
}
