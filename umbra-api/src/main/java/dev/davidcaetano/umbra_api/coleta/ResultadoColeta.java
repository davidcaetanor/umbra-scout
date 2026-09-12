package dev.davidcaetano.umbra_api.coleta;

import java.util.List;

public record ResultadoColeta(List<ProdutoColetado> produtos, int totalElegivel, int totalSemPreco) {
}
