package dev.davidcaetano.umbra_api.coleta.itad;

public class ItadApiException extends RuntimeException {

    private final int statusCode;
    private final String reasonPhrase;

    public ItadApiException(int statusCode, String reasonPhrase, Throwable cause) {
        super("ITAD API respondeu %d: %s".formatted(statusCode, reasonPhrase), cause);
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    public int statusCode() {
        return statusCode;
    }

    public String reasonPhrase() {
        return reasonPhrase;
    }
}
