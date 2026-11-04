package dev.davidcaetano.umbra_api.catalogo.service;

import java.util.List;

public record JogoExterno(String gid, String nome, String imagemUrl, List<OfertaExterna> ofertas) {
}
