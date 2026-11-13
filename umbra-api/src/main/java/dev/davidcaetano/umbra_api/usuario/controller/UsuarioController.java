package dev.davidcaetano.umbra_api.usuario.controller;

import dev.davidcaetano.umbra_api.usuario.dto.response.UsuarioResponse;
import dev.davidcaetano.umbra_api.usuario.service.UsuarioAutenticado;
import dev.davidcaetano.umbra_api.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/eu")
    public UsuarioResponse eu(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioService.buscar(usuario);
    }
}
