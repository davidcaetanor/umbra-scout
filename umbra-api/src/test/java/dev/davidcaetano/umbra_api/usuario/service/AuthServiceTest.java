package dev.davidcaetano.umbra_api.usuario.service;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import dev.davidcaetano.umbra_api.usuario.EmailJaCadastradoException;
import dev.davidcaetano.umbra_api.usuario.entity.UsuarioEntity;
import dev.davidcaetano.umbra_api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class AuthServiceTest extends IntegrationTestBase {

    private static final Instant AGORA = Instant.parse("2026-11-13T15:00:00Z");
    private static final String SENHA = "senha-forte-de-teste";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);

    @Autowired
    private UsuarioRepository usuarioRepository;

    private AuthService authService;

    @BeforeEach
    void criarServico() {
        authService = new AuthService(usuarioRepository, passwordEncoder, clock);
    }

    @Test
    void deveCadastrarComEmailNormalizadoESenhaEmHash() {
        UsuarioEntity usuario = authService.cadastrar(" David@Exemplo.com ", SENHA, "David");

        UsuarioEntity gravado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(gravado.getEmail()).isEqualTo("david@exemplo.com");
        assertThat(gravado.getNomeExibicao()).isEqualTo("David");
        assertThat(gravado.getSenhaHash()).isNotEqualTo(SENHA);
        assertThat(passwordEncoder.matches(SENHA, gravado.getSenhaHash())).isTrue();
        assertThat(gravado.getDiscordId()).isNull();
        assertThat(gravado.getCriadoEm()).isEqualTo(OffsetDateTime.ofInstant(AGORA, ZoneOffset.UTC));
    }

    @Test
    void deveRecusarEmailJaCadastradoIgnorandoMaiusculas() {
        authService.cadastrar("david@exemplo.com", SENHA, "David");

        assertThatThrownBy(() -> authService.cadastrar("DAVID@exemplo.com", SENHA, "Outro"))
                .isInstanceOf(EmailJaCadastradoException.class);
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void deveTraduzirViolacaoDoEmailUnicoQuandoOutroCadastroGravouPrimeiro() {
        usuarioRepository.saveAndFlush(UsuarioEntity.comSenha("david@exemplo.com", "hash", "David",
                OffsetDateTime.ofInstant(AGORA, ZoneOffset.UTC)));
        AuthService servicoQueNaoViuOOutroCadastro =
                new AuthService(repositorioQueNaoEncontraEmail(), passwordEncoder, clock);

        assertThatThrownBy(() -> servicoQueNaoViuOOutroCadastro.cadastrar("david@exemplo.com", SENHA, "David"))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    void deveRepassarViolacaoQueNaoEDoEmailUnico() {
        assertThatThrownBy(() -> authService.cadastrar("sem-arroba", SENHA, "David"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_usuario_email");
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
