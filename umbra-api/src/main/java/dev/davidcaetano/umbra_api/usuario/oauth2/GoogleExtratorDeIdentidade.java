package dev.davidcaetano.umbra_api.usuario.oauth2;

import dev.davidcaetano.umbra_api.usuario.service.IdentidadeExterna;
import dev.davidcaetano.umbra_api.usuario.service.ProvedorDeIdentidade;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class GoogleExtratorDeIdentidade implements ExtratorDeIdentidade {

    @Override
    public ProvedorDeIdentidade provedor() {
        return ProvedorDeIdentidade.GOOGLE;
    }

    @Override
    public IdentidadeExterna extrair(OAuth2User usuario) {
        return new IdentidadeExterna(provedor(), String.valueOf((Object) usuario.getAttribute("sub")),
                usuario.getAttribute("email"), Boolean.TRUE.equals(usuario.getAttribute("email_verified")),
                usuario.getAttribute("name"));
    }
}
