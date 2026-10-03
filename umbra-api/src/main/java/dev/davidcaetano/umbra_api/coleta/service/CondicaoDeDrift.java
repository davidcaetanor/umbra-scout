package dev.davidcaetano.umbra_api.coleta.service;

public enum CondicaoDeDrift {

    RESPOSTA_VAZIA(SituacaoRodada.REJEITADA),
    SEM_PRECO_ACIMA_DO_LIMIAR(SituacaoRodada.REJEITADA),
    NENHUM_ELEGIVEL(SituacaoRodada.REJEITADA),
    COBERTURA_ABAIXO_DO_DECLARADO(SituacaoRodada.DEGRADADA),
    DUPLICATA_ENTRE_PAGINAS(SituacaoRodada.DEGRADADA);

    private final SituacaoRodada efeito;

    CondicaoDeDrift(SituacaoRodada efeito) {
        this.efeito = efeito;
    }

    public SituacaoRodada efeito() {
        return efeito;
    }
}
