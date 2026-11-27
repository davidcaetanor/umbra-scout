package dev.davidcaetano.umbra_api.notificacao;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AvaliadorDeAlvo {

    private static final DecisaoDeAlvo MANTER = new Manter();
    private static final DecisaoDeAlvo REARMAR = new Rearmar();

    private final Duration tetoEntreAvisos;

    public AvaliadorDeAlvo(Duration tetoEntreAvisos) {
        Objects.requireNonNull(tetoEntreAvisos, "tetoEntreAvisos é obrigatório");
        if (tetoEntreAvisos.isNegative() || tetoEntreAvisos.isZero()) {
            throw new IllegalArgumentException("tetoEntreAvisos deve ser positivo");
        }
        this.tetoEntreAvisos = tetoEntreAvisos;
    }

    public DecisaoDeAlvo avaliar(EstadoDoAviso estado, List<OfertaAvaliada> ofertas, Instant agora) {
        Objects.requireNonNull(estado, "estado é obrigatório");
        Objects.requireNonNull(ofertas, "ofertas é obrigatório");
        Objects.requireNonNull(agora, "agora é obrigatório");

        if (estado.foraDeVenda()) {
            return MANTER;
        }

        List<OfertaAvaliada> validas = ofertas.stream()
                .filter(OfertaAvaliada::valeParaAviso)
                .sorted(Comparator.comparingLong(OfertaAvaliada::valorCentavos).thenComparingLong(OfertaAvaliada::ofertaId))
                .toList();
        if (validas.isEmpty()) {
            return MANTER;
        }

        long melhorPreco = validas.getFirst().valorCentavos();
        if (melhorPreco > estado.precoAlvoCentavos()) {
            return estado.armado() ? MANTER : REARMAR;
        }

        boolean novidade = estado.armado() || melhorPreco < estado.precoAvisadoCentavos();
        if (!novidade || !tetoVencido(estado.ultimoAvisoEm(), agora)) {
            return MANTER;
        }

        List<OfertaAvaliada> cruzaram = validas.stream()
                .filter(oferta -> oferta.valorCentavos() <= estado.precoAlvoCentavos())
                .toList();
        MotivoDoAviso motivo = estado.armado() ? MotivoDoAviso.ENTRADA : MotivoDoAviso.QUEDA;
        return new Avisar(motivo, melhorPreco, cruzaram);
    }

    private boolean tetoVencido(Instant ultimoAvisoEm, Instant agora) {
        return ultimoAvisoEm == null || !agora.isBefore(ultimoAvisoEm.plus(tetoEntreAvisos));
    }
}
