package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;

public interface Scraper {

    OrigemColeta fonte();

    Cobertura cobertura();

    ResultadoColeta coletar();
}
