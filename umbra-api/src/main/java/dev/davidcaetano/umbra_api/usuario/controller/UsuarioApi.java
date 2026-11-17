package dev.davidcaetano.umbra_api.usuario.controller;

import dev.davidcaetano.umbra_api.comum.openapi.RespostaDeErro;
import dev.davidcaetano.umbra_api.usuario.dto.response.UsuarioResponse;
import dev.davidcaetano.umbra_api.usuario.service.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Usuário", description = "Conta do usuário do token.")
public interface UsuarioApi {

    @Operation(operationId = "buscarUsuarioAtual", summary = "Conta do usuário do token")
    @ApiResponse(responseCode = "200", description = "Conta ativa.")
    @ApiResponse(responseCode = "404", ref = RespostaDeErro.REF_NAO_ENCONTRADO)
    UsuarioResponse eu(@Parameter(hidden = true) UsuarioAutenticado usuario);
}
