package dev.davidcaetano.umbra_api.coleta.kabum;

import dev.davidcaetano.umbra_api.coleta.kabum.dto.response.KabumCatalogoResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KabumClientImpl implements KabumClient {

    private static final String SORT = "name";
    private static final int PAGE_SIZE = 120;
    private static final Map<String, List<?>> FILTRO_CATALOGO = filtroCatalogo();

    private final RestClient kabumRestClient;
    private final ObjectMapper objectMapper;

    private static Map<String, List<?>> filtroCatalogo() {
        Map<String, List<?>> filtro = new LinkedHashMap<>();
        filtro.put("category", List.of("Hardware"));
        filtro.put("kabum_product", List.of(true));
        return filtro;
    }

    @Override
    @RateLimiter(name = "kabum")
    public KabumCatalogoResponse buscarPaginaHardware(int pageNumber) {
        String facetFilters = codificarFacetFilters();

        try {
            return kabumRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/catalog/v2/products")
                            .queryParam("facet_filters", facetFilters)
                            .queryParam("sort", SORT)
                            .queryParam("page_number", pageNumber)
                            .queryParam("page_size", PAGE_SIZE)
                            .build())
                    .retrieve()
                    .body(KabumCatalogoResponse.class);
        } catch (RestClientException ex) {
            throw traduzirErro(ex);
        }
    }

    private String codificarFacetFilters() {
        String json = objectMapper.writeValueAsString(FILTRO_CATALOGO);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private KabumApiException traduzirErro(RestClientException ex) {
        if (ex instanceof RestClientResponseException responseEx) {
            String corpo = responseEx.getResponseBodyAsString();
            String mensagem = corpo.isBlank() ? responseEx.getMessage() : corpo;

            log.warn("Kabum Catalog API respondeu erro: {} - {}", responseEx.getStatusCode(), mensagem);
            return new KabumApiException(responseEx.getStatusCode().value(), mensagem, ex);
        }

        log.warn("Kabum Catalog API devolveu corpo que nao pode ser convertido: {}", ex.getMessage());
        return new KabumApiException(0, ex.getMessage(), ex);
    }
}
