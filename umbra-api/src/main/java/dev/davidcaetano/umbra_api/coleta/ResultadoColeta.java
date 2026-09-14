package dev.davidcaetano.umbra_api.coleta;

import java.util.List;

public record ResultadoColeta(List<OfertaColetada> ofertas, int totalElegivel, int totalSemPreco) {
}
