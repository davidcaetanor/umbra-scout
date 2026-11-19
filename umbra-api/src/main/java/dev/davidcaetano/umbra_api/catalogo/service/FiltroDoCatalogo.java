package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.enums.CategoriaHardware;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;

public record FiltroDoCatalogo(String termo, TipoProduto tipo, CategoriaHardware categoria) {
}
