package dev.davidcaetano.umbra_api.notificacao;

import java.util.List;
import java.util.Objects;

public record Avisar(MotivoDoAviso motivo, long melhorPrecoCentavos, List<OfertaAvaliada> cruzaram)
        implements DecisaoDeAlvo {

    public Avisar {
        Objects.requireNonNull(motivo, "motivo é obrigatório");
        cruzaram = List.copyOf(cruzaram);
        if (cruzaram.isEmpty()) {
            throw new IllegalArgumentException("cruzaram deve ter ao menos uma oferta");
        }
    }
}
