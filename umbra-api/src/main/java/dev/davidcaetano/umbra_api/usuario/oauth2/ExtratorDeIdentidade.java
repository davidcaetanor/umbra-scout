package dev.davidcaetano.umbra_api.usuario.oauth2;

import dev.davidcaetano.umbra_api.usuario.service.IdentidadeExterna;
import dev.davidcaetano.umbra_api.usuario.service.ProvedorDeIdentidade;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface ExtratorDeIdentidade {

    ProvedorDeIdentidade provedor();

    IdentidadeExterna extrair(OAuth2User usuario);
}
