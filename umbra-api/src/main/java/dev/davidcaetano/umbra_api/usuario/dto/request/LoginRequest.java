package dev.davidcaetano.umbra_api.usuario.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

public record LoginRequest(
        @NotBlank(message = "é obrigatório")
        @Size(max = 320, message = "deve ter no máximo 320 caracteres")
        String email,
        @NotNull(message = "é obrigatória")
        String senha) {

    private static final int LIMITE_DO_BCRYPT_EM_BYTES = 72;

    public LoginRequest {
        email = email == null ? null : email.strip();
    }

    @AssertTrue(message = "senha deve ter no máximo 72 bytes")
    public boolean isSenhaDentroDoLimiteDoBcrypt() {
        return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= LIMITE_DO_BCRYPT_EM_BYTES;
    }

    @Override
    public String toString() {
        return "LoginRequest[email=%s]".formatted(email);
    }
}
