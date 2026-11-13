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
        CadastroRequest request = cadastro("  usuario_xanisvaldo@exemplo.com ", SENHA, " Xanisvaldo ");

        assertThat(validador.validate(request)).isEmpty();
        assertThat(request.email()).isEqualTo(EMAIL);
        assertThat(request.nomeExibicao()).isEqualTo("Xanisvaldo");
    }

    @Test
    void deveRecusarSenhaComMenosDe8Caracteres() {
        assertThat(camposInvalidos(cadastro(EMAIL, "1234567", "Xanisvaldo")))
                .containsExactly("senha");
    }

    @Test
    void deveAceitarSenhaDe72BytesERecusarDe73() {
        assertThat(camposInvalidos(cadastro(EMAIL, "a".repeat(72), "Xanisvaldo"))).isEmpty();
        assertThat(camposInvalidos(cadastro(EMAIL, "a".repeat(73), "Xanisvaldo")))
                .containsExactly("senhaDentroDoLimiteDoBcrypt");
    }

    @Test
    void deveContarOLimiteDaSenhaEmBytesENaoEmCaracteres() {
        String trintaESeteCaracteresComDoisBytesCada = "ç".repeat(37);

        assertThat(camposInvalidos(cadastro(EMAIL, trintaESeteCaracteresComDoisBytesCada,
                "Xanisvaldo"))).containsExactly("senhaDentroDoLimiteDoBcrypt");
    }

    @Test
    void deveRecusarEmailInvalidoENomeVazio() {
        assertThat(camposInvalidos(cadastro("sem-arroba", SENHA, "  ")))
                .containsExactlyInAnyOrder("email", "nomeExibicao");
    }

    @Test
    void deveRecusarCamposAusentes() {
        assertThat(camposInvalidos(cadastro(null, null, null)))
                .containsExactlyInAnyOrder("email", "senha", "nomeExibicao");
    }

    @Test
    void deveRecusarNomeExibicaoComMaisDe60Caracteres() {
        assertThat(camposInvalidos(cadastro(EMAIL, SENHA, "a".repeat(61))))
                .containsExactly("nomeExibicao");
    }

    @Test
    void toStringNaoDeveExporASenha() {
        assertThat(cadastro(EMAIL, SENHA, "Xanisvaldo").toString())
                .contains(EMAIL)
                .doesNotContain(SENHA);
    }

    @Test
    void deveNormalizarCepComHifenPontoEEspacos() {
        assertThat(cadastroComCep(" 01.310-100 ").cep()).isEqualTo("01310100");
        assertThat(camposInvalidos(cadastroComCep("01310-100"))).isEmpty();
    }

    @Test
    void deveTratarCepAusenteOuSoComMascaraComoNaoInformado() {
        assertThat(cadastroComCep(null).cep()).isNull();
        assertThat(cadastroComCep(" - ").cep()).isNull();
        assertThat(camposInvalidos(cadastroComCep(" - "))).isEmpty();
    }

    @Test
    void deveRecusarCepForaDoFormatoDepoisDeNormalizar() {
        assertThat(camposInvalidos(cadastroComCep("0131010"))).containsExactly("cep");
        assertThat(camposInvalidos(cadastroComCep("013101000"))).containsExactly("cep");
        assertThat(camposInvalidos(cadastroComCep("abcde-fgh"))).containsExactly("cep");
        assertThat(camposInvalidos(cadastroComCep("01310/100"))).containsExactly("cep");
    }

    @Test
    void toStringNaoDeveExporOCep() {
        assertThat(cadastroComCep("01310100").toString()).doesNotContain("01310100");
    }

    private static CadastroRequest cadastro(String email, String senha, String nomeExibicao) {
        return new CadastroRequest(email, senha, nomeExibicao, null);
    }

    private static CadastroRequest cadastroComCep(String cep) {
        return new CadastroRequest(EMAIL, SENHA, "Xanisvaldo", cep);
    }

    private static Set<String> camposInvalidos(CadastroRequest request) {
        Set<ConstraintViolation<CadastroRequest>> violacoes = validador.validate(request);
        return violacoes.stream()
                .map(violacao -> violacao.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
