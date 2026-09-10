package dev.davidcaetano.umbra_api.coleta.kabum;

import dev.davidcaetano.umbra_api.coleta.kabum.dto.response.KabumCatalogoResponse;

public interface KabumClient {

    KabumCatalogoResponse buscarPaginaHardware(int pageNumber, int pageSize);

}
