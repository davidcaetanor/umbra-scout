package dev.davidcaetano.umbra_api.config.log;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filtro = new RequestIdFilter();

    @Test
    void deveGerarRequestIdEColocaNoMdcDuranteChamada() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> capturadoDuranteChamada = new AtomicReference<>();

        FilterChain chain = (req, res) -> capturadoDuranteChamada.set(MDC.get(RequestIdFilter.MDC_KEY));

        filtro.doFilter(request, response, chain);

        assertThat(capturadoDuranteChamada.get()).isNotBlank();
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(capturadoDuranteChamada.get());
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void deveReaproveitarRequestIdRecebidoHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "id-vindo-do-proxy");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> capturado = new AtomicReference<>();

        FilterChain chain = (req, res) -> capturado.set(MDC.get(RequestIdFilter.MDC_KEY));

        filtro.doFilter(request, response, chain);

        assertThat(capturado.get()).isEqualTo("id-vindo-do-proxy");
    }
}