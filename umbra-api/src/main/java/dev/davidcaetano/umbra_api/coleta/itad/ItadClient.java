package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadDescobertaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ItadClient {

    ItadDescobertaResponse buscarDescoberta(int offset);

    Map<UUID, List<String>> resolverIdentificadorNativo(int shopId, List<UUID> gids);

    Map<String, UUID> resolverGidPorIdentificador(int shopId, List<String> identificadoresItad);

    List<ItadPrecoJogoResponse> buscarPrecos(List<UUID> gids);
}
