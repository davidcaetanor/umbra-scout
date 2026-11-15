package dev.davidcaetano.umbra_api.usuario.service;

import dev.davidcaetano.umbra_api.usuario.CodigoDeTrocaInvalidoException;
import dev.davidcaetano.umbra_api.usuario.LoginExternoRecusadoException;
import dev.davidcaetano.umbra_api.usuario.dto.response.TokenResponse;
import dev.davidcaetano.umbra_api.usuario.entity.UsuarioEntity;
import dev.davidcaetano.umbra_api.usuario.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
public class LoginExternoService {

    private static final int TAMANHO_MAXIMO_DO_NOME = 60;

    private final UsuarioRepository usuarioRepository;
    private final CodigosDeTroca codigosDeTroca;
    private final JwtService jwtService;
    private final Clock clock;

    public LoginExternoService(UsuarioRepository usuarioRepository, CodigosDeTroca codigosDeTroca,
                               JwtService jwtService, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.codigosDeTroca = codigosDeTroca;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    public String entrar(IdentidadeExterna identidade) {
        return codigosDeTroca.emitir(resolverConta(identidade));
    }

    public TokenResponse trocar(String codigo) {
        Long usuarioId = codigosDeTroca.resgatar(codigo).orElseThrow(CodigoDeTrocaInvalidoException::new);
        return TokenResponse.bearer(jwtService.emitir(usuarioId), clock.getZone());
    }

    Long resolverConta(IdentidadeExterna identidade) {
        try {
            return resolverUmaVez(identidade);
        } catch (DataIntegrityViolationException corrida) {
            log.info("Login externo concorrente, conta relida: provedor={}", identidade.provedor());
            return resolverUmaVez(identidade);
        }
    }

    private Long resolverUmaVez(IdentidadeExterna identidade) {
        ProvedorDeIdentidade provedor = identidade.provedor();
        Optional<UsuarioEntity> vinculado = provedor.buscar(usuarioRepository, identidade.idExterno());
        if (vinculado.isPresent()) {
            return exigirAtiva(vinculado.get()).getId();
        }

        if (!identidade.emailVerificado() || identidade.email() == null || identidade.email().isBlank()) {
            throw new LoginExternoRecusadoException(MotivoDeRecusa.EMAIL_NAO_VERIFICADO);
        }
        String email = UsuarioEntity.normalizarEmail(identidade.email());
        OffsetDateTime agora = OffsetDateTime.now(clock);

        Optional<UsuarioEntity> mesmoEmail = usuarioRepository.findByEmail(email);
        if (mesmoEmail.isPresent()) {
            return vincular(exigirAtiva(mesmoEmail.get()), identidade, agora);
        }

        UsuarioEntity novo = provedor.criar(email, identidade.idExterno(), nomeExibicao(identidade, email), agora);
        Long id = usuarioRepository.saveAndFlush(novo).getId();
        log.info("Conta criada por login externo: provedor={}, usuarioId={}", provedor, id);
        return id;
    }

    private Long vincular(UsuarioEntity usuario, IdentidadeExterna identidade, OffsetDateTime agora) {
        ProvedorDeIdentidade provedor = identidade.provedor();
        if (provedor.jaVinculado(usuario)) {
            throw new LoginExternoRecusadoException(MotivoDeRecusa.CONTA_VINCULADA_A_OUTRA_IDENTIDADE);
        }
        provedor.vincular(usuario, identidade.idExterno(), agora);
        Long id = usuarioRepository.saveAndFlush(usuario).getId();
        log.info("Login externo vinculado a conta existente: provedor={}, usuarioId={}", provedor, id);
        return id;
    }

    private static UsuarioEntity exigirAtiva(UsuarioEntity usuario) {
        if (!usuario.isAtivo()) {
            throw new LoginExternoRecusadoException(MotivoDeRecusa.CONTA_INATIVA);
        }
        return usuario;
    }

    private static String nomeExibicao(IdentidadeExterna identidade, String email) {
        String nome = identidade.nome() == null || identidade.nome().isBlank()
                ? email.substring(0, email.indexOf('@'))
                : identidade.nome().trim();
        return nome.length() > TAMANHO_MAXIMO_DO_NOME ? nome.substring(0, TAMANHO_MAXIMO_DO_NOME) : nome;
    }
}
