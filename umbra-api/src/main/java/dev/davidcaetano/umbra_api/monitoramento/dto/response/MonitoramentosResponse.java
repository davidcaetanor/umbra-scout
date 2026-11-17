package dev.davidcaetano.umbra_api.monitoramento.dto.response;

import java.util.List;

public record MonitoramentosResponse(
        List<MonitoramentoResponse> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas) {
}
