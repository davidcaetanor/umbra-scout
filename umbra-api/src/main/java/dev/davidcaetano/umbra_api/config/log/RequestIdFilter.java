package dev.davidcaetano.umbra_api.config.log;

import dev.davidcaetano.umbra_api.comum.log.ChaveDeLog;
import dev.davidcaetano.umbra_api.comum.log.SanitizacaoDeLog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestIdFilter.class);

    public static final String MDC_KEY = ChaveDeLog.REQUEST_ID.chave();
    public static final String HEADER = "X-Request-Id";
    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private String obterOuGerarRequestId(HttpServletRequest request) {
        String recebido = request.getHeader(HEADER);
        return (recebido != null && UUID_PATTERN.matcher(recebido).matches()) ? recebido : UUID.randomUUID().toString();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = obterOuGerarRequestId(request);
        try {
            MDC.put(MDC_KEY, requestId);
            response.setHeader(HEADER, requestId);
            LOGGER.info("Requisição recebida: {} {}", SanitizacaoDeLog.paraLog(request.getMethod()), SanitizacaoDeLog.paraLog(request.getRequestURI()));
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
