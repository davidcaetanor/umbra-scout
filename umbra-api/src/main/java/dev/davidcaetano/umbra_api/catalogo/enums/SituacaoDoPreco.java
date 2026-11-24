package dev.davidcaetano.umbra_api.catalogo.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SituacaoDoPreco {

    VIGENTE("vigente"),
    PROMOCAO_NO_PRAZO("promocao-no-prazo"),
    VERIFICACAO_ATRASADA("verificacao-atrasada"),
    PROMOCAO_VENCIDA_SEM_COLETA("promocao-vencida-sem-coleta");

    private final String codigo;

    SituacaoDoPreco(String codigo) {
        this.codigo = codigo;
    }

    @JsonValue
    public String codigo() {
        return codigo;
    }

    public boolean valeParaAviso() {
        return this == VIGENTE || this == PROMOCAO_NO_PRAZO;
    }
}
