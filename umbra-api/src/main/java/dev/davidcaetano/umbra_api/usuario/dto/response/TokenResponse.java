package dev.davidcaetano.umbra_api.usuario.dto.response;

import dev.davidcaetano.umbra_api.usuario.service.TokenEmitido;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record TokenResponse(String token, String tipo, OffsetDateTime expiraEm) {

    private static final String TIPO_BEARER = "Bearer";

    public static TokenResponse bearer(TokenEmitido token, ZoneId zona) {
        return new TokenResponse(token.valor(), TIPO_BEARER, OffsetDateTime.ofInstant(token.expiraEm(), zona));
    }
}
