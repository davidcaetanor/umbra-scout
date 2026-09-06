package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;

import java.util.List;

public interface Scraper {

    OrigemColeta fonte();

    List<ProdutoColetado> coletar();
}
