package dev.davidcaetano.umbra_api.usuario.dto.response;

import java.time.OffsetDateTime;

public record TokenResponse(String token, String tipo, OffsetDateTime expiraEm) {
}
