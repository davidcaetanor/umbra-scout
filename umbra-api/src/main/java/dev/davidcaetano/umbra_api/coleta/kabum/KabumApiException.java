package dev.davidcaetano.umbra_api.coleta.kabum;

public class KabumApiException extends RuntimeException {

    private final int statusCode;

    public KabumApiException(int statusCode, String message, Throwable cause) {
        super("Kabum Catalog API respondeu %d: %s".formatted(statusCode, message), cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

}
