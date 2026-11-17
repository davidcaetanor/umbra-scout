package dev.davidcaetano.umbra_api.usuario.dto.response;

import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;

public record UsuarioResponse(
        Long id,
        String email,
        String nomeExibicao,
        @Nullable String cep,
        @Nullable String cidade,
        @Nullable String uf,
        OffsetDateTime criadoEm) {
}
