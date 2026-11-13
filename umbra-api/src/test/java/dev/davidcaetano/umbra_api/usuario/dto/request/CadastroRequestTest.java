package dev.davidcaetano.umbra_api.usuario.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CadastroRequestTest {

    private static final String EMAIL = "usuario_xanisvaldo@exemplo.com";
    private static final String SENHA = "senha-forte";

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void criarValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        fabrica.close();
    }

    @Test
    void deveAceitarCadastroValidoERemoverEspacosDasPontas() {
        CadastroRequest request = new CadastroRequest("  usuario_xanisvaldo@exemplo.com ", SENHA, " Xanisvaldo ");

        assertThat(validador.validate(request)).isEmpty();
        assertThat(request.email()).isEqualTo(EMAIL);
        assertThat(request.nomeExibicao()).isEqualTo("Xanisvaldo");
    }

    @Test
    void deveRecusarSenhaComMenosDe8Caracteres() {
        assertThat(camposInvalidos(new CadastroRequest(EMAIL, "1234567", "Xanisvaldo")))
                .containsExactly("senha");
    }

    @Test
    void deveAceitarSenhaDe72BytesERecusarDe73() {
        assertThat(camposInvalidos(new CadastroRequest(EMAIL, "a".repeat(72), "Xanisvaldo"))).isEmpty();
        assertThat(camposInvalidos(new CadastroRequest(EMAIL, "a".repeat(73), "Xanisvaldo")))
                .containsExactly("senhaDentroDoLimiteDoBcrypt");
    }

    @Test
    void deveContarOLimiteDaSenhaEmBytesENaoEmCaracteres() {
        String trintaESeteCaracteresComDoisBytesCada = "ç".repeat(37);

        assertThat(camposInvalidos(new CadastroRequest(EMAIL, trintaESeteCaracteresComDoisBytesCada,
                "Xanisvaldo"))).containsExactly("senhaDentroDoLimiteDoBcrypt");
    }

    @Test
    void deveRecusarEmailInvalidoENomeVazio() {
        assertThat(camposInvalidos(new CadastroRequest("sem-arroba", SENHA, "  ")))
                .containsExactlyInAnyOrder("email", "nomeExibicao");
    }

    @Test
    void deveRecusarCamposAusentes() {
        assertThat(camposInvalidos(new CadastroRequest(null, null, null)))
                .containsExactlyInAnyOrder("email", "senha", "nomeExibicao");
    }

    @Test
    void deveRecusarNomeExibicaoComMaisDe60Caracteres() {
        assertThat(camposInvalidos(new CadastroRequest(EMAIL, SENHA, "a".repeat(61))))
                .containsExactly("nomeExibicao");
    }

    @Test
    void toStringNaoDeveExporASenha() {
        assertThat(new CadastroRequest(EMAIL, SENHA, "Xanisvaldo").toString())
                .contains(EMAIL)
                .doesNotContain(SENHA);
    }

    private static Set<String> camposInvalidos(CadastroRequest request) {
        Set<ConstraintViolation<CadastroRequest>> violacoes = validador.validate(request);
        return violacoes.stream()
                .map(violacao -> violacao.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
