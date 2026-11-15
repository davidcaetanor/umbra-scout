package dev.davidcaetano.umbra_api.usuario.service;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import dev.davidcaetano.umbra_api.usuario.CodigoDeTrocaInvalidoException;
import dev.davidcaetano.umbra_api.usuario.LoginExternoRecusadoException;
import dev.davidcaetano.umbra_api.usuario.dto.response.TokenResponse;
import dev.davidcaetano.umbra_api.usuario.entity.UsuarioEntity;
import dev.davidcaetano.umbra_api.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class LoginExternoServiceTest extends IntegrationTestBase {

    private static final String EMAIL = "jogadora@exemplo.com";
    private static final String ID_EXTERNO = "80351110224678912";
    private static final Instant AGORA = Instant.parse("2026-11-15T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AGORA, ZoneOffset.UTC);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private final CodigosDeTroca codigosDeTroca = new CodigosDeTroca();
    private LoginExternoService service;

    @BeforeEach
    void montarServico() {
        service = servico(usuarioRepository);
    }

    @ParameterizedTest
    @EnumSource(ProvedorDeIdentidade.class)
    void primeiroLogin_deveCriarContaSemSenhaJaComOIdDoProvedor(ProvedorDeIdentidade provedor) {
        Long id = service.resolverConta(identidade(provedor, EMAIL, true));

        UsuarioEntity criado = usuarioRepository.findById(id).orElseThrow();
        assertThat(criado.getEmail()).isEqualTo(EMAIL);
        assertThat(criado.getSenhaHash()).isNull();
        assertThat(provedor.buscar(usuarioRepository, ID_EXTERNO)).map(UsuarioEntity::getId).contains(id);
        assertThat(criado.getNomeExibicao()).isEqualTo("Jogadora");
    }

    @ParameterizedTest
    @EnumSource(ProvedorDeIdentidade.class)
    void segundoLogin_deveEntrarNaMesmaConta(ProvedorDeIdentidade provedor) {
        Long primeiro = service.resolverConta(identidade(provedor, EMAIL, true));

        Long segundo = service.resolverConta(identidade(provedor, "email-trocado@exemplo.com", false));

        assertThat(segundo).isEqualTo(primeiro);
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(ProvedorDeIdentidade.class)
    void emailDeContaLocal_deveVincularEmVezDeDuplicar(ProvedorDeIdentidade provedor) {
        UsuarioEntity local = usuarioRepository.saveAndFlush(
                UsuarioEntity.comSenha(EMAIL, "$2a$12$hash", "Local", OffsetDateTime.now(CLOCK)));

        Long id = service.resolverConta(identidade(provedor, "Jogadora@Exemplo.com", true));

        assertThat(id).isEqualTo(local.getId());
        assertThat(usuarioRepository.count()).isEqualTo(1);
        UsuarioEntity vinculado = provedor.buscar(usuarioRepository, ID_EXTERNO).orElseThrow();
        assertThat(vinculado.getSenhaHash()).isEqualTo("$2a$12$hash");
        assertThat(vinculado.getNomeExibicao()).isEqualTo("Local");
    }

    @ParameterizedTest
    @EnumSource(ProvedorDeIdentidade.class)
    void emailNaoVerificado_naoDeveVincularNemCriar(ProvedorDeIdentidade provedor) {
        usuarioRepository.saveAndFlush(UsuarioEntity.comSenha(EMAIL, "$2a$12$hash", "Local", OffsetDateTime.now(CLOCK)));

        assertThatThrownBy(() -> service.resolverConta(identidade(provedor, EMAIL, false)))
                .isInstanceOfSatisfying(LoginExternoRecusadoException.class,
                        ex -> assertThat(ex.getMotivo()).isEqualTo(MotivoDeRecusa.EMAIL_NAO_VERIFICADO));
        assertThat(provedor.buscar(usuarioRepository, ID_EXTERNO)).isEmpty();
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void provedorSemEmail_deveSerRecusado() {
        assertThatThrownBy(() -> service.resolverConta(identidade(ProvedorDeIdentidade.DISCORD, null, true)))
                .isInstanceOfSatisfying(LoginExternoRecusadoException.class,
                        ex -> assertThat(ex.getMotivo()).isEqualTo(MotivoDeRecusa.EMAIL_NAO_VERIFICADO));
        assertThat(usuarioRepository.count()).isZero();
    }

    @ParameterizedTest
    @EnumSource(ProvedorDeIdentidade.class)
    void contaVinculadaInativa_deveSerRecusada(ProvedorDeIdentidade provedor) {
        Long id = service.resolverConta(identidade(provedor, EMAIL, true));
        desativar(id);

        assertThatThrownBy(() -> service.resolverConta(identidade(provedor, EMAIL, true)))
                .isInstanceOfSatisfying(LoginExternoRecusadoException.class,
                        ex -> assertThat(ex.getMotivo()).isEqualTo(MotivoDeRecusa.CONTA_INATIVA));
    }

    @Test
    void contaLocalInativa_naoDeveSerVinculada() {
        UsuarioEntity local = usuarioRepository.saveAndFlush(
                UsuarioEntity.comSenha(EMAIL, "$2a$12$hash", "Local", OffsetDateTime.now(CLOCK)));
        desativar(local.getId());

        assertThatThrownBy(() -> service.resolverConta(identidade(ProvedorDeIdentidade.GOOGLE, EMAIL, true)))
                .isInstanceOfSatisfying(LoginExternoRecusadoException.class,
                        ex -> assertThat(ex.getMotivo()).isEqualTo(MotivoDeRecusa.CONTA_INATIVA));
    }

    @Test
    void emailJaVinculadoAOutraContaDoMesmoProvedor_naoDeveTrocarOVinculo() {
        usuarioRepository.saveAndFlush(UsuarioEntity.comDiscord(EMAIL, "outro-id", "Outra", OffsetDateTime.now(CLOCK)));

        assertThatThrownBy(() -> service.resolverConta(identidade(ProvedorDeIdentidade.DISCORD, EMAIL, true)))
                .isInstanceOfSatisfying(LoginExternoRecusadoException.class, ex -> assertThat(ex.getMotivo())
                        .isEqualTo(MotivoDeRecusa.CONTA_VINCULADA_A_OUTRA_IDENTIDADE));
        assertThat(usuarioRepository.findByDiscordId("outro-id")).isPresent();
    }

    @Test
    void contaComOutroProvedor_deveReceberOSegundoVinculo() {
        Long discord = service.resolverConta(identidade(ProvedorDeIdentidade.DISCORD, EMAIL, true));

        Long google = service.resolverConta(identidade(ProvedorDeIdentidade.GOOGLE, EMAIL, true));

        assertThat(google).isEqualTo(discord);
        UsuarioEntity usuario = usuarioRepository.findById(discord).orElseThrow();
        assertThat(usuario.getDiscordId()).isEqualTo(ID_EXTERNO);
        assertThat(usuario.getGoogleId()).isEqualTo(ID_EXTERNO);
    }

    @Test
    void nomeAusente_deveUsarOInicioDoEmail_eNomeLongo_deveSerCortadoEm60() {
        Long semNome = service.resolverConta(
                new IdentidadeExterna(ProvedorDeIdentidade.DISCORD, "1", "sem.nome@exemplo.com", true, " "));
        Long nomeLongo = service.resolverConta(
                new IdentidadeExterna(ProvedorDeIdentidade.GOOGLE, "2", "longo@exemplo.com", true, "n".repeat(80)));

        assertThat(usuarioRepository.findById(semNome).orElseThrow().getNomeExibicao()).isEqualTo("sem.nome");
        assertThat(usuarioRepository.findById(nomeLongo).orElseThrow().getNomeExibicao()).hasSize(60);
    }

    @Test
    void criacaoConcorrente_deveRelerAContaCriadaPeloOutroLogin() {
        AtomicBoolean primeiraGravacao = new AtomicBoolean(true);
        UsuarioRepository comCorrida = (UsuarioRepository) Proxy.newProxyInstance(
                UsuarioRepository.class.getClassLoader(), new Class<?>[]{UsuarioRepository.class},
                (proxy, metodo, argumentos) -> {
                    if (metodo.getName().equals("saveAndFlush") && primeiraGravacao.getAndSet(false)) {
                        usuarioRepository.saveAndFlush(UsuarioEntity.comDiscord(EMAIL, ID_EXTERNO, "Concorrente",
                                OffsetDateTime.now(CLOCK)));
                        throw new DataIntegrityViolationException("uq_usuario_discord");
                    }
                    try {
                        return metodo.invoke(usuarioRepository, argumentos);
                    } catch (InvocationTargetException ex) {
                        throw ex.getCause();
                    }
                });

        Long id = servico(comCorrida).resolverConta(identidade(ProvedorDeIdentidade.DISCORD, EMAIL, true));

        assertThat(usuarioRepository.findByDiscordId(ID_EXTERNO)).map(UsuarioEntity::getId).contains(id);
        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void entrarETrocar_deveEmitirJwtDoUsuarioUmaVezSo() {
        String codigo = service.entrar(identidade(ProvedorDeIdentidade.DISCORD, EMAIL, true));

        TokenResponse token = service.trocar(codigo);

        assertThat(token.tipo()).isEqualTo("Bearer");
        assertThat(token.token()).isNotBlank();
        assertThat(token.expiraEm()).isEqualTo(OffsetDateTime.ofInstant(AGORA.plus(Duration.ofHours(1)), ZoneOffset.UTC));
        assertThatThrownBy(() -> service.trocar(codigo)).isInstanceOf(CodigoDeTrocaInvalidoException.class);
    }

    @Test
    void codigoDesconhecido_deveSerRecusado() {
        assertThatThrownBy(() -> service.trocar("inexistente")).isInstanceOf(CodigoDeTrocaInvalidoException.class);
    }

    private void desativar(Long id) {
        jdbc.update("UPDATE usuario SET ativo = false WHERE id = ?", id);
        entityManager.clear();
    }

    private LoginExternoService servico(UsuarioRepository repositorio) {
        var chave = new SecretKeySpec("segredo-de-teste-com-pelo-menos-32-bytes".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        var jwtService = new JwtService(NimbusJwtEncoder.withSecretKey(chave).algorithm(MacAlgorithm.HS256).build(),
                "umbra-api", Duration.ofHours(1), CLOCK);
        return new LoginExternoService(repositorio, codigosDeTroca, jwtService, CLOCK);
    }

    private static IdentidadeExterna identidade(ProvedorDeIdentidade provedor, String email, boolean verificado) {
        return new IdentidadeExterna(provedor, ID_EXTERNO, email, verificado, "Jogadora");
    }
}
