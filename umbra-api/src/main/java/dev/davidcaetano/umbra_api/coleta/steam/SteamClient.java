package dev.davidcaetano.umbra_api.coleta.steam;

import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamAppDetalhesResponse;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamDescobertaResponse;

public interface SteamClient {

    SteamDescobertaResponse buscarDescoberta();

    SteamAppDetalhesResponse buscarDetalhes(int appid);

}
