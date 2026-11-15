package dev.davidcaetano.umbra_api.usuario.oauth2.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@ConfigurationProperties(prefix = "umbra.login-externo")
@Validated
public record LoginExternoProperties(
        @NotNull(message = "umbra.login-externo.retorno-front é obrigatório (UMBRA_LOGIN_RETORNO_FRONT)")
        URI retornoFront) {

    public String retornoComCodigo(String codigo) {
        return retornoCom("codigo", codigo);
    }

    public String retornoComErro(String erro) {
        return retornoCom("erro", erro);
    }

    private String retornoCom(String parametro, String valor) {
        return UriComponentsBuilder.fromUri(retornoFront).queryParam(parametro, valor).build().toUriString();
    }
}
