package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.coleta.OfertaColetada;

import java.util.List;

record GrupoDeOfertas(GrupoChave chave, List<OfertaColetada> ofertas) {
}
