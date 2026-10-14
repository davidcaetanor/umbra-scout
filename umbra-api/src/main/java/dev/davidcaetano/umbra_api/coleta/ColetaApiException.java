package dev.davidcaetano.umbra_api.coleta;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.OptionalInt;

public abstract class ColetaApiException extends RuntimeException {

    private static final int MENOR_STATUS_DE_ERRO_DO_SERVIDOR = 500;

    private final TipoFalhaColeta tipo;

    protected ColetaApiException(String fonte, String detalhe, RestClientException causa) {
        this(fonte, detalhe, causa, classificar(causa));
    }

    private ColetaApiException(String fonte, String detalhe, RestClientException causa, TipoFalhaColeta tipo) {
        super(montarMensagem(fonte, detalhe, tipo, statusDa(causa)), causa);
        this.tipo = tipo;
    }

    public TipoFalhaColeta tipo() {
        return tipo;
    }

    public OptionalInt status() {
        return statusDa(getCause());
    }

    public boolean transitoria() {
        return switch (tipo) {
            case FALHA_DE_IO -> true;
            case RESPOSTA_HTTP -> status().getAsInt() >= MENOR_STATUS_DE_ERRO_DO_SERVIDOR;
            case CORPO_INVALIDO -> false;
        };
    }

    private static TipoFalhaColeta classificar(RestClientException causa) {
        if (causa instanceof RestClientResponseException) {
            return TipoFalhaColeta.RESPOSTA_HTTP;
        }
        if (causa instanceof ResourceAccessException) {
            return TipoFalhaColeta.FALHA_DE_IO;
        }
        return TipoFalhaColeta.CORPO_INVALIDO;
    }

    private static OptionalInt statusDa(Throwable causa) {
        return causa instanceof RestClientResponseException resposta
                ? OptionalInt.of(resposta.getStatusCode().value())
                : OptionalInt.empty();
    }

    private static String montarMensagem(String fonte, String detalhe, TipoFalhaColeta tipo, OptionalInt status) {
        return switch (tipo) {
            case RESPOSTA_HTTP -> "%s respondeu %d: %s".formatted(fonte, status.getAsInt(), detalhe);
            case FALHA_DE_IO -> "%s nao respondeu (falha de I/O): %s".formatted(fonte, detalhe);
            case CORPO_INVALIDO -> "%s devolveu corpo que nao pode ser convertido: %s".formatted(fonte, detalhe);
        };
    }
}
