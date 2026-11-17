package dev.davidcaetano.umbra_api.usuario.controller;

import dev.davidcaetano.umbra_api.comum.openapi.RespostaDeErro;
import dev.davidcaetano.umbra_api.usuario.dto.request.CadastroRequest;
import dev.davidcaetano.umbra_api.usuario.dto.request.LoginRequest;
import dev.davidcaetano.umbra_api.usuario.dto.request.TrocaDeCodigoRequest;
import dev.davidcaetano.umbra_api.usuario.dto.response.TokenResponse;
import dev.davidcaetano.umbra_api.usuario.dto.response.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Autenticação", description = "Cadastro, login por email e senha e login por provedor externo.")
@SecurityRequirements
public interface AuthApi {

    @Operation(operationId = "cadastrar", summary = "Cria conta com email e senha",
            description = "CEP opcional, aceito com ou sem máscara; a cidade vem do ViaCEP quando disponível.")
    @ApiResponse(responseCode = "201", description = "Conta criada.")
    @ApiResponse(responseCode = "400", ref = RespostaDeErro.REF_PARAMETRO_INVALIDO)
    @ApiResponse(responseCode = "409", ref = RespostaDeErro.REF_EM_CONFLITO)
    UsuarioResponse cadastrar(CadastroRequest cadastro);

    @Operation(operationId = "entrar", summary = "Login por email e senha")
    @ApiResponse(responseCode = "200", description = "Token de acesso.")
    @ApiResponse(responseCode = "400", ref = RespostaDeErro.REF_PARAMETRO_INVALIDO)
    @ApiResponse(responseCode = "401", ref = RespostaDeErro.REF_NAO_AUTENTICADO)
    TokenResponse entrar(LoginRequest login);

    @Operation(operationId = "trocarCodigoDeLoginExterno", summary = "Troca o código do login externo por token",
            description = "O código chega ao frontend no retorno de `/login/oauth2/code/{provedor}`, vale uma vez "
                    + "e expira em pouco tempo.")
    @ApiResponse(responseCode = "200", description = "Token de acesso.")
    @ApiResponse(responseCode = "400", ref = RespostaDeErro.REF_PARAMETRO_INVALIDO)
    @ApiResponse(responseCode = "401", ref = RespostaDeErro.REF_NAO_AUTENTICADO)
    TokenResponse trocarCodigo(TrocaDeCodigoRequest troca);
}
