package dev.davidcaetano.umbra_api.usuario.service;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import dev.davidcaetano.umbra_api.usuario.CredenciaisInvalidasException;
import dev.davidcaetano.umbra_api.usuario.EmailJaCadastradoException;
import dev.davidcaetano.umbra_api.usuario.dto.request.CadastroRequest;
import dev.davidcaetano.umbra_api.usuario.dto.request.LoginRequest;
import dev.davidcaetano.umbra_api.usuario.dto.response.TokenResponse;
import dev.davidcaetano.umbra_api.usuario.dto.response.UsuarioResponse;
import dev.davidcaetano.umbra_api.usuario.entity.UsuarioEntity;
import dev.davidcaetano.umbra_api.usuario.mapper.UsuarioMapperImpl;
import dev.davidcaetano.umbra_api.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ExtendWith(OutputCaptureExtension.class)
class AuthServiceTest extends IntegrationTestBase {

    private static final String EMAIL = "usuario_xanisvaldo@exemplo.com";
    private static final Instant AGORA = Instant.parse("2026-11-13T15:00:00Z");
    private static final String SENHA = "senha-forte-de-teste";
    private static final String CEP = "01310100";
    private static final RegiaoDoCep REGIAO = new RegiaoDoCep("São Paulo", "SP");

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
    private static final SecretKey CHAVE =
            new SecretKeySpec("k".repeat(32).getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private final JwtService jwtService = new JwtService(
            NimbusJwtEncoder.withSecretKey(CHAVE).algorithm(MacAlgorithm.HS256).build(), "umbra-api",
            Duration.ofHours(6), clock);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final List<String> cepsConsultados = new ArrayList<>();

    private AuthService authService;

    @BeforeEach
    void criarServico() {
        authService = servico(cep -> Optional.of(REGIAO));
    }

    @Test
    void deveCadastrarComEmailNormalizadoESenhaEmHash() {
        UsuarioResponse usuario = authService.cadastrar(cadastro("Usuario_Xanisvaldo@Exemplo.com", "Xanisvaldo"));

        assertThat(usuario.email()).isEqualTo(EMAIL);
        assertThat(usuario.nomeExibicao()).isEqualTo("Xanisvaldo");
        UsuarioEntity gravado = usuarioRepository.findById(usuario.id()).orElseThrow();
        assertThat(gravado.getEmail()).isEqualTo(EMAIL);
        assertThat(gravado.getNomeExibicao()).isEqualTo("Xanisvaldo");
        assertThat(gravado.getSenhaHash()).isNotEqualTo(SENHA);
        assertThat(passwordEncoder.matches(SENHA, gravado.getSenhaHash())).isTrue();
        assertThat(gravado.getDiscordId()).isNull();
        assertThat(gravado.getCriadoEm()).isEqualTo(OffsetDateTime.ofInstant(AGORA, ZoneOffset.UTC));
    }

    @Test
    void deveRecusarEmailJaCadastradoIgnorandoMaiusculas() {
        authService.cadastrar(cadastro(EMAIL, "Xanisvaldo"));

        assertThatThrownBy(() -> authService.cadastrar(cadastro("USUARIO_XANISVALDO@exemplo.com", "Outro")))
                .isInstanceOf(EmailJaCadastradoException.class);
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void deveTraduzirViolacaoDoEmailUnicoQuandoOutroCadastroGravouPrimeiro() {
        usuarioRepository.saveAndFlush(UsuarioEntity.comSenha(EMAIL, "hash", "Xanisvaldo",
                OffsetDateTime.ofInstant(AGORA, ZoneOffset.UTC)));
        AuthService servicoQueNaoViuOOutroCadastro =
                new AuthService(repositorioQueNaoEncontraEmail(), new UsuarioMapperImpl(), passwordEncoder,
                        cep -> Optional.empty(), jwtService, clock);

        assertThatThrownBy(() -> servicoQueNaoViuOOutroCadastro.cadastrar(cadastro(EMAIL, "Xanisvaldo")))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    void deveRepassarViolacaoQueNaoEDoEmailUnico() {
        assertThatThrownBy(() -> authService.cadastrar(cadastro("sem-arroba", "Xanisvaldo")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_usuario_email");
    }

    @Test
    void cepEncontrado_deveGravarCidadeEUfEDevolverNaResposta() {
        UsuarioResponse usuario = authService.cadastrar(cadastroComCep(CEP));

        assertThat(usuario.cep()).isEqualTo(CEP);
        assertThat(usuario.cidade()).isEqualTo("São Paulo");
        assertThat(usuario.uf()).isEqualTo("SP");
        UsuarioEntity gravado = usuarioRepository.findById(usuario.id()).orElseThrow();
        assertThat(gravado.getCep()).isEqualTo(CEP);
        assertThat(gravado.getCidade()).isEqualTo("São Paulo");
        assertThat(gravado.getUf()).isEqualTo("SP");
    }

    @Test
    void cepInexistente_deveCadastrarComCepSemCidadeNemUf(CapturedOutput saida) {
        UsuarioResponse usuario = servico(cep -> Optional.empty()).cadastrar(cadastroComCep("99999999"));

        UsuarioEntity gravado = usuarioRepository.findById(usuario.id()).orElseThrow();
        assertThat(gravado.getCep()).isEqualTo("99999999");
        assertThat(gravado.getCidade()).isNull();
        assertThat(gravado.getUf()).isNull();
        assertThat(saida.getAll()).doesNotContain("Consulta de CEP indisponível");
    }

    @Test
    void consultaIndisponivel_deveCadastrarSemCidadeNemUfELogarWarnSemOCep(CapturedOutput saida) {
        AuthService servicoSemViaCep = servico(cep -> {
            throw new ConsultaDeCepIndisponivelException("ViaCEP indisponível: ResourceAccessException");
        });

        UsuarioResponse usuario = servicoSemViaCep.cadastrar(cadastroComCep(CEP));

        UsuarioEntity gravado = usuarioRepository.findById(usuario.id()).orElseThrow();
        assertThat(gravado.getCep()).isEqualTo(CEP);
        assertThat(gravado.getCidade()).isNull();
        assertThat(gravado.getUf()).isNull();
        assertThat(saida.getAll())
                .contains("WARN")
                .contains("ViaCEP indisponível: ResourceAccessException")
                .doesNotContain(CEP);
    }

    @Test
    void semCep_naoDeveConsultar() {
        UsuarioResponse usuario = authService.cadastrar(cadastro(EMAIL, "Xanisvaldo"));

        assertThat(cepsConsultados).isEmpty();
        assertThat(usuario.cep()).isNull();
        assertThat(usuario.cidade()).isNull();
        assertThat(usuario.uf()).isNull();
    }

    @Test
    void emailJaCadastrado_naoDeveConsultarOCep() {
        authService.cadastrar(cadastro(EMAIL, "Xanisvaldo"));

        assertThatThrownBy(() -> authService.cadastrar(cadastroComCep(CEP)))
                .isInstanceOf(EmailJaCadastradoException.class);
        assertThat(cepsConsultados).isEmpty();
    }

    @Test
    void loginValido_deveEmitirTokenDoUsuarioComValidadeConfigurada() {
        UsuarioResponse usuario = authService.cadastrar(cadastro(EMAIL, "Xanisvaldo"));

        TokenResponse token = authService.entrar(new LoginRequest("Usuario_Xanisvaldo@Exemplo.com", SENHA));

        assertThat(token.tipo()).isEqualTo("Bearer");
        assertThat(token.expiraEm()).isEqualTo(OffsetDateTime.ofInstant(AGORA.plus(Duration.ofHours(6)), ZoneOffset.UTC));
        Jwt jwt = NimbusJwtDecoder.withSecretKey(CHAVE).macAlgorithm(MacAlgorithm.HS256).build().decode(token.token());
        assertThat(jwt.getSubject()).isEqualTo(usuario.id().toString());
        assertThat(jwt.getClaims()).doesNotContainKey("email");
    }

    @Test
    void senhaErrada_deveRecusarComCredenciaisInvalidas() {
        authService.cadastrar(cadastro(EMAIL, "Xanisvaldo"));

        assertThatThrownBy(() -> authService.entrar(new LoginRequest(EMAIL, "senha-errada-do-teste")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Email ou senha inválidos.");
    }

    @Test
    void emailDesconhecido_deveRecusarComAMesmaMensagemDaSenhaErrada() {
        assertThatThrownBy(() -> authService.entrar(new LoginRequest("ninguem@exemplo.com", SENHA)))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Email ou senha inválidos.");
    }

    @Test
    void contaSemSenha_deveRecusarLoginPorSenha() {
        usuarioRepository.saveAndFlush(UsuarioEntity.comDiscord(EMAIL, "123456789", "Xanisvaldo",
                OffsetDateTime.ofInstant(AGORA, ZoneOffset.UTC)));

        assertThatThrownBy(() -> authService.entrar(new LoginRequest(EMAIL, SENHA)))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void contaInativa_deveRecusarMesmoComSenhaCerta() {
        UsuarioResponse usuario = authService.cadastrar(cadastro(EMAIL, "Xanisvaldo"));
        jdbc.update("UPDATE usuario SET ativo = false WHERE id = ?", usuario.id());
        entityManager.clear();

        assertThatThrownBy(() -> authService.entrar(new LoginRequest(EMAIL, SENHA)))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    private AuthService servico(ConsultaDeCep consultaDeCep) {
        ConsultaDeCep registrandoConsultas = cep -> {
            cepsConsultados.add(cep);
            return consultaDeCep.consultar(cep);
        };
        return new AuthService(usuarioRepository, new UsuarioMapperImpl(), passwordEncoder, registrandoConsultas,
                jwtService, clock);
    }

    private static CadastroRequest cadastro(String email, String nomeExibicao) {
        return new CadastroRequest(email, SENHA, nomeExibicao, null);
    }

    private static CadastroRequest cadastroComCep(String cep) {
        return new CadastroRequest(EMAIL, SENHA, "Xanisvaldo", cep);
    }

    private UsuarioRepository repositorioQueNaoEncontraEmail() {
        return (UsuarioRepository) Proxy.newProxyInstance(UsuarioRepository.class.getClassLoader(),
                new Class<?>[]{UsuarioRepository.class},
                (proxy, metodo, argumentos) -> {
                    if (metodo.getName().equals("existsByEmail")) {
                        return false;
                    }
                    try {
                        return metodo.invoke(usuarioRepository, argumentos);
                    } catch (InvocationTargetException ex) {
                        throw ex.getCause();
                    }
                });
    }
}
