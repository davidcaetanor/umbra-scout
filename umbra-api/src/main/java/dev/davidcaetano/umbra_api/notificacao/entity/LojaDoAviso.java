package dev.davidcaetano.umbra_api.notificacao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record LojaDoAviso(
        @Column(name = "oferta_id", nullable = false) Long ofertaId,
        @Column(name = "valor_centavos", nullable = false) long valorCentavos) {
}
