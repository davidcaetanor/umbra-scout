package dev.davidcaetano.umbra_api.usuario.dto.response;

import java.time.OffsetDateTime;

public record UsuarioResponse(
        Long id,
        String email,
        String nomeExibicao,
        OffsetDateTime criadoEm) {
}
