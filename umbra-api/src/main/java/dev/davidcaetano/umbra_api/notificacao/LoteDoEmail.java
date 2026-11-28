package dev.davidcaetano.umbra_api.notificacao;

import java.util.List;

public record LoteDoEmail(List<ItemDoEmail> itens, int restantes, List<Long> avisosEnviados,
                          List<Long> avisosDescartados) {

    public LoteDoEmail {
        itens = List.copyOf(itens);
        avisosEnviados = List.copyOf(avisosEnviados);
        avisosDescartados = List.copyOf(avisosDescartados);
    }

    public boolean vazio() {
        return itens.isEmpty();
    }

    public int totalDeAlvos() {
        return itens.size() + restantes;
    }
}
