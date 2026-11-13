package dev.davidcaetano.umbra_api.usuario.service;

import dev.davidcaetano.umbra_api.usuario.UsuarioNaoEncontradoException;
import dev.davidcaetano.umbra_api.usuario.dto.response.UsuarioResponse;
import dev.davidcaetano.umbra_api.usuario.mapper.UsuarioMapper;
import dev.davidcaetano.umbra_api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(UsuarioAutenticado usuario) {
        return usuarioRepository.findByIdAndAtivoTrue(usuario.id())
                .map(usuarioMapper::paraResponse)
                .orElseThrow(UsuarioNaoEncontradoException::new);
    }
}
