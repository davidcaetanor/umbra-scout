package dev.davidcaetano.umbra_api.usuario.service;

import dev.davidcaetano.umbra_api.usuario.CredenciaisInvalidasException;
import dev.davidcaetano.umbra_api.usuario.EmailJaCadastradoException;
import dev.davidcaetano.umbra_api.usuario.dto.request.CadastroRequest;
import dev.davidcaetano.umbra_api.usuario.dto.request.LoginRequest;
import dev.davidcaetano.umbra_api.usuario.dto.response.TokenResponse;
import dev.davidcaetano.umbra_api.usuario.dto.response.UsuarioResponse;
import dev.davidcaetano.umbra_api.usuario.entity.UsuarioEntity;
import dev.davidcaetano.umbra_api.usuario.mapper.UsuarioMapper;
import dev.davidcaetano.umbra_api.usuario.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private static final String RESTRICAO_EMAIL_UNICO = "uq_usuario_email";
    private static final String TIPO_DO_TOKEN = "Bearer";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final ConsultaDeCep consultaDeCep;
    private final JwtService jwtService;
    private final Clock clock;
    private final String hashParaEmailDesconhecido;

    public AuthService(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper,
                       PasswordEncoder passwordEncoder, ConsultaDeCep consultaDeCep, JwtService jwtService,
                       Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.passwordEncoder = passwordEncoder;
        this.consultaDeCep = consultaDeCep;
        this.jwtService = jwtService;
        this.clock = clock;
        this.hashParaEmailDesconhecido = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public UsuarioResponse cadastrar(CadastroRequest cadastro) {
        String emailNormalizado = UsuarioEntity.normalizarEmail(cadastro.email());

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new EmailJaCadastradoException();
        }

        UsuarioEntity usuario = UsuarioEntity.comSenha(emailNormalizado, passwordEncoder.encode(cadastro.senha()),
                cadastro.nomeExibicao(), OffsetDateTime.now(clock));
        informarCep(usuario, cadastro.cep());

        try {
            return usuarioMapper.paraResponse(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException ex) {
            if (violouEmailUnico(ex)) {
                throw new EmailJaCadastradoException();
            }
            throw ex;
        }
    }

    public TokenResponse entrar(LoginRequest login) {
        Optional<UsuarioEntity> usuario = usuarioRepository.findByEmail(UsuarioEntity.normalizarEmail(login.email()))
                .filter(UsuarioEntity::isAtivo);
        Optional<String> senhaHash = usuario.map(UsuarioEntity::getSenhaHash);
        boolean senhaConfere = passwordEncoder.matches(login.senha(), senhaHash.orElse(hashParaEmailDesconhecido));

        if (senhaHash.isEmpty() || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }

        TokenEmitido token = jwtService.emitir(usuario.get().getId());
        return new TokenResponse(token.valor(), TIPO_DO_TOKEN, OffsetDateTime.ofInstant(token.expiraEm(), clock.getZone()));
    }

    private void informarCep(UsuarioEntity usuario, String cep) {
        if (cep == null) {
            return;
        }
        consultarRegiao(cep).ifPresentOrElse(
                regiao -> usuario.informarCepComRegiao(cep, regiao.cidade(), regiao.uf()),
                () -> usuario.informarCep(cep));
    }

    private Optional<RegiaoDoCep> consultarRegiao(String cep) {
        try {
            return consultaDeCep.consultar(cep);
        } catch (ConsultaDeCepIndisponivelException ex) {
            log.warn("Consulta de CEP indisponível, cadastro segue sem cidade e UF: causa={}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static boolean violouEmailUnico(DataIntegrityViolationException ex) {
        return ex.getCause() instanceof ConstraintViolationException violacao
                && RESTRICAO_EMAIL_UNICO.equals(violacao.getConstraintName());
    }
}
