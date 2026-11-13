package dev.davidcaetano.umbra_api.config.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String DESAFIO_BEARER = "Bearer";

    private final HandlerExceptionResolver handlerExceptionResolver;

    public ProblemDetailAuthenticationEntryPoint(HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, DESAFIO_BEARER);
        handlerExceptionResolver.resolveException(request, response, null, authException);
    }
}
