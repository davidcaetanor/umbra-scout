package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;

import java.util.EnumSet;
import java.util.Set;

final class JulgamentoDeRodada {

    static final int AMOSTRA_MINIMA = 10;
    static final int LIMIAR_SEM_PRECO_PERCENTUAL = 20;
    static final int MARGEM_COBERTURA_PERCENTUAL = 2;
    static final int MARGEM_DUPLICATA_PERCENTUAL = 2;

    VereditoRodada julgar(ResultadoColeta resultado) {

        Reconciliacao reconciliacao = resultado.reconciliacao();
        Long declarado = reconciliacao.declarado();
        long brutos = reconciliacao.brutos();
        long distintos = reconciliacao.distintos();
        long elegiveis = resultado.totalElegivel();
        long semPreco = resultado.totalSemPreco();

        Set<CondicaoDeDrift> condicoes = EnumSet.noneOf(CondicaoDeDrift.class);

        if (brutos == 0) {
            condicoes.add(CondicaoDeDrift.RESPOSTA_VAZIA);
        }

        if (brutos >= AMOSTRA_MINIMA && elegiveis == 0) {
            condicoes.add(CondicaoDeDrift.NENHUM_ELEGIVEL);
        }

        if (elegiveis >= AMOSTRA_MINIMA && semPreco * 100 > elegiveis * LIMIAR_SEM_PRECO_PERCENTUAL) {
            condicoes.add(CondicaoDeDrift.SEM_PRECO_ACIMA_DO_LIMIAR);
        }

        if (declarado != null && (declarado - distintos) * 100 > declarado * MARGEM_COBERTURA_PERCENTUAL) {
            condicoes.add(CondicaoDeDrift.COBERTURA_ABAIXO_DO_DECLARADO);
        }

        if ((brutos - distintos) * 100 > brutos * MARGEM_DUPLICATA_PERCENTUAL) {
            condicoes.add(CondicaoDeDrift.DUPLICATA_ENTRE_PAGINAS);
        }

        return new VereditoRodada(condicoes);
    }
}
