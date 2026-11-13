package dev.davidcaetano.umbra_api.usuario.service;

import dev.davidcaetano.umbra_api.usuario.EmailJaCadastradoException;
import dev.davidcaetano.umbra_api.usuario.dto.request.CadastroRequest;
import dev.davidcaetano.umbra_api.usuario.dto.response.UsuarioResponse;
import dev.davidcaetano.umbra_api.usuario.entity.UsuarioEntity;
import dev.davidcaetano.umbra_api.usuario.mapper.UsuarioMapper;
import dev.davidcaetano.umbra_api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String RESTRICAO_EMAIL_UNICO = "uq_usuario_email";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final ConsultaDeCep consultaDeCep;
    private final Clock clock;

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
