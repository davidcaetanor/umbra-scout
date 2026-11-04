package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadErroResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

@Slf4j
final class TraducaoDeErroItad {

    private TraducaoDeErroItad() {
    }

    static <T> T executar(Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (RestClientException ex) {
            throw traduzirErro(ex);
        }
    }

    private static ItadApiException traduzirErro(RestClientException ex) {
        ItadApiException erro = new ItadApiException(detalhe(ex), ex);
        log.warn("{}", erro.getMessage());
        return erro;
    }

    private static String detalhe(RestClientException ex) {
        if (!(ex instanceof RestClientResponseException resposta)) {
            return ex.getMessage();
        }
        String reasonPhrase = reasonPhraseDoCorpo(resposta);
        if (reasonPhrase != null && !reasonPhrase.isBlank()) {
            return reasonPhrase;
        }
        String corpo = resposta.getResponseBodyAsString();
        return corpo.isBlank() ? resposta.getMessage() : corpo;
    }

    private static String reasonPhraseDoCorpo(RestClientResponseException resposta) {
        try {
            ItadErroResponse erro = resposta.getResponseBodyAs(ItadErroResponse.class);
            return erro == null ? null : erro.reasonPhrase();
        } catch (RuntimeException naoDecodificou) {
            log.debug("Corpo de erro do ITAD nao pode ser decodificado como ItadErroResponse", naoDecodificou);
            return null;
        }
    }
}
