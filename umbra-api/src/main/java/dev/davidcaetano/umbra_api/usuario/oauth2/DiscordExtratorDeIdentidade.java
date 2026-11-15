package dev.davidcaetano.umbra_api.usuario.oauth2;

import dev.davidcaetano.umbra_api.usuario.service.IdentidadeExterna;
import dev.davidcaetano.umbra_api.usuario.service.ProvedorDeIdentidade;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class DiscordExtratorDeIdentidade implements ExtratorDeIdentidade {

    @Override
    public ProvedorDeIdentidade provedor() {
        return ProvedorDeIdentidade.DISCORD;
    }

    @Override
    public IdentidadeExterna extrair(OAuth2User usuario) {
        String nomeGlobal = usuario.getAttribute("global_name");
        String nome = nomeGlobal != null ? nomeGlobal : usuario.getAttribute("username");
        return new IdentidadeExterna(provedor(), String.valueOf((Object) usuario.getAttribute("id")),
                usuario.getAttribute("email"), Boolean.TRUE.equals(usuario.getAttribute("verified")), nome);
    }
}
