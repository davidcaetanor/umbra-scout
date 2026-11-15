package dev.davidcaetano.umbra_api.usuario.oauth2;

import dev.davidcaetano.umbra_api.usuario.LoginExternoRecusadoException;
import dev.davidcaetano.umbra_api.usuario.oauth2.properties.LoginExternoProperties;
import dev.davidcaetano.umbra_api.usuario.service.LoginExternoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LoginExternoSucessoHandler implements AuthenticationSuccessHandler {

    private final Map<String, ExtratorDeIdentidade> extratoresPorRegistro;
    private final LoginExternoService loginExternoService;
    private final LoginExternoProperties properties;

    public LoginExternoSucessoHandler(List<ExtratorDeIdentidade> extratores, LoginExternoService loginExternoService,
                                      LoginExternoProperties properties) {
        this.extratoresPorRegistro = extratores.stream()
                .collect(Collectors.toUnmodifiableMap(extrator -> extrator.provedor().idDeRegistro(),
                        Function.identity()));
        this.loginExternoService = loginExternoService;
        this.properties = properties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken autenticacao = (OAuth2AuthenticationToken) authentication;
        String destino;
        try {
            String codigo = loginExternoService.entrar(
                    extrator(autenticacao.getAuthorizedClientRegistrationId()).extrair(autenticacao.getPrincipal()));
            destino = properties.retornoComCodigo(codigo);
        } catch (LoginExternoRecusadoException ex) {
            log.warn("Login externo recusado: provedor={}, motivo={}",
                    autenticacao.getAuthorizedClientRegistrationId(), ex.getMotivo().codigo());
            destino = properties.retornoComErro(ex.getMotivo().codigo());
        } finally {
            EncerramentoDeSessao.encerrar(request, response);
        }
        response.sendRedirect(destino);
    }

    private ExtratorDeIdentidade extrator(String idDeRegistro) {
        ExtratorDeIdentidade extrator = extratoresPorRegistro.get(idDeRegistro);
        if (extrator == null) {
            throw new IllegalStateException("Registro OAuth2 sem extrator de identidade: " + idDeRegistro);
        }
        return extrator;
    }
}
