package dev.davidcaetano.umbra_api.usuario.oauth2;

import dev.davidcaetano.umbra_api.usuario.oauth2.properties.LoginExternoProperties;
import dev.davidcaetano.umbra_api.usuario.service.MotivoDeRecusa;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginExternoFalhaHandler implements AuthenticationFailureHandler {

    private final LoginExternoProperties properties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String erro = exception instanceof OAuth2AuthenticationException oauth2
                ? oauth2.getError().getErrorCode()
                : exception.getClass().getSimpleName();
        EncerramentoDeSessao.encerrar(request, response);
        response.sendRedirect(properties.retornoComErro(motivoDoErro(erro).codigo()));
    }

    private static MotivoDeRecusa motivoDoErro(String erro) {
        if (OAuth2ErrorCodes.ACCESS_DENIED.equals(erro)) {
            log.info("Login externo cancelado pelo usuario no provedor");
            return MotivoDeRecusa.ENTRADA_CANCELADA;
        }
        log.warn("Login externo falhou no provedor: erro={}", erro);
        return MotivoDeRecusa.FALHA_NO_PROVEDOR;
    }
}
