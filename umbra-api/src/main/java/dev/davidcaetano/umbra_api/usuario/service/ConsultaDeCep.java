package dev.davidcaetano.umbra_api.usuario.service;

import java.util.Optional;

public interface ConsultaDeCep {

    Optional<RegiaoDoCep> consultar(String cep);
}
