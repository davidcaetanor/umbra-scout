package dev.davidcaetano.umbra_api.usuario.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;

public record CadastroRequest(
        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um email válido")
        @Size(max = 320, message = "deve ter no máximo 320 caracteres")
        String email,
        @NotNull(message = "é obrigatória")
        @Size(min = 8, message = "deve ter pelo menos 8 caracteres")
        String senha,
        @NotBlank(message = "é obrigatório")
        @Size(max = 60, message = "deve ter no máximo 60 caracteres")
        String nomeExibicao,
        @Pattern(regexp = "[0-9]{8}", message = "deve ter 8 dígitos")
        String cep) {

    private static final int LIMITE_DO_BCRYPT_EM_BYTES = 72;
    private static final String MASCARA_DO_CEP = "[\\s.-]";

    public CadastroRequest {
        email = email == null ? null : email.strip();
        nomeExibicao = nomeExibicao == null ? null : nomeExibicao.strip();
        cep = normalizarCep(cep);
    }

    private static String normalizarCep(String cep) {
        if (cep == null) {
            return null;
        }
        String semMascara = cep.replaceAll(MASCARA_DO_CEP, "");
        return semMascara.isEmpty() ? null : semMascara;
    }

    @AssertTrue(message = "senha deve ter no máximo 72 bytes")
    public boolean isSenhaDentroDoLimiteDoBcrypt() {
        return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= LIMITE_DO_BCRYPT_EM_BYTES;
    }

    @Override
    public String toString() {
        return "CadastroRequest[email=%s, nomeExibicao=%s]".formatted(email, nomeExibicao);
    }
}
