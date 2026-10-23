package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties.Limiares;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
final class JulgamentoDeRodada {

    private final AlarmeColetaProperties alarme;

    JulgamentoDeRodada(AlarmeColetaProperties alarme) {
        this.alarme = alarme;
    }

    VereditoRodada julgar(ResultadoColeta resultado) {

        Limiares limiares = alarme.limiaresDe(resultado.fonte());
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

        if (brutos >= limiares.amostraMinima() && elegiveis == 0) {
            condicoes.add(CondicaoDeDrift.NENHUM_ELEGIVEL);
        }

        if (elegiveis >= limiares.amostraMinima()
                && semPreco * 100 > elegiveis * limiares.limiarSemPrecoPercentual()) {
            condicoes.add(CondicaoDeDrift.SEM_PRECO_ACIMA_DO_LIMIAR);
        }

        if (declarado != null && (declarado - distintos) * 100 > declarado * limiares.margemCoberturaPercentual()) {
            condicoes.add(CondicaoDeDrift.COBERTURA_ABAIXO_DO_DECLARADO);
        }

        if ((brutos - distintos) * 100 > brutos * limiares.margemDuplicataPercentual()) {
            condicoes.add(CondicaoDeDrift.DUPLICATA_ENTRE_PAGINAS);
        }

        return new VereditoRodada(condicoes);
    }
}
