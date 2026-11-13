package dev.davidcaetano.umbra_api.usuario.controller;

import dev.davidcaetano.umbra_api.usuario.dto.request.CadastroRequest;
import dev.davidcaetano.umbra_api.usuario.dto.response.UsuarioResponse;
import dev.davidcaetano.umbra_api.usuario.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse cadastrar(@Valid @RequestBody CadastroRequest cadastro) {
        return authService.cadastrar(cadastro);
    }
}
