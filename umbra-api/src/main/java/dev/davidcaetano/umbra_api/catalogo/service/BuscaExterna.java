package dev.davidcaetano.umbra_api.catalogo.service;

import java.util.List;

public interface BuscaExterna {

    List<JogoExterno> buscar(String termo);
}
