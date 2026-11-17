package dev.davidcaetano.umbra_api.usuario.service;

public enum MotivoDeRecusa {

    EMAIL_NAO_VERIFICADO("email_nao_verificado"),
    CONTA_INATIVA("conta_inativa"),
    CONTA_VINCULADA_A_OUTRA_IDENTIDADE("conta_vinculada_a_outra_identidade"),
    FALHA_NO_PROVEDOR("falha_no_provedor");

    private final String codigo;

    MotivoDeRecusa(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
