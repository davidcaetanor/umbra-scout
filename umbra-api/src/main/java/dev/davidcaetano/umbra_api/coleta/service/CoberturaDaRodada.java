package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;

enum CoberturaDaRodada {

    COMPLETA,
    DENTRO_DA_MARGEM,
    INSUFICIENTE;

    static CoberturaDaRodada de(ResultadoColeta resultado, VereditoRodada veredito) {
        boolean rodadaConfiavel = resultado.cobertura().ehCenso()
                && veredito.situacao() == SituacaoRodada.SAUDAVEL
                && resultado.totalSemPreco() == 0;

        if (!rodadaConfiavel) {
            return INSUFICIENTE;
        }

        Reconciliacao reconciliacao = resultado.reconciliacao();
        Long declarado = reconciliacao.declarado();

        return declarado == null || reconciliacao.distintos() >= declarado ? COMPLETA : DENTRO_DA_MARGEM;
    }
}
