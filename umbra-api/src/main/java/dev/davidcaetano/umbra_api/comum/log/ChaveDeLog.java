package dev.davidcaetano.umbra_api.comum.log;

public enum ChaveDeLog {

    REQUEST_ID("requestId"),
    RODADA_ID("rodadaId");

    private final String chave;

    ChaveDeLog(String chave) {
        this.chave = chave;
    }

    public String chave() {
        return chave;
    }
}
