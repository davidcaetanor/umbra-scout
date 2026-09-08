package dev.davidcaetano.umbra_api.coleta.steam;

public class SteamApiException extends RuntimeException {

    private final int statusCode;

    public SteamApiException(int statusCode, String message, Throwable cause) {
        super("Steam Storefront API respondeu %d: %s".formatted(statusCode, message), cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }

}
