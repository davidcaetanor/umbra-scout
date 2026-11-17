package dev.davidcaetano.umbra_api.monitoramento.controller;

import dev.davidcaetano.umbra_api.comum.openapi.RespostaDeErro;
import dev.davidcaetano.umbra_api.monitoramento.dto.request.AlterarMonitoramentoRequest;
import dev.davidcaetano.umbra_api.monitoramento.dto.request.CriarMonitoramentoRequest;
import dev.davidcaetano.umbra_api.monitoramento.dto.response.MonitoramentoResponse;
import dev.davidcaetano.umbra_api.monitoramento.dto.response.MonitoramentosResponse;
import dev.davidcaetano.umbra_api.usuario.service.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "Monitoramento", description = "Preço-alvo por produto, sempre do usuário do token. Conta desativada "
        + "recebe 404 em todas as rotas.")
public interface MonitoramentoApi {

    @Operation(operationId = "criarMonitoramento", summary = "Monitora um produto",
            description = "Informe `produtoId` de um produto do catálogo ou `gid` de um jogo achado no ITAD, "
                    + "nunca os dois. Jogo pelo `gid` fora do catálogo é incluído antes de criar o monitoramento.")
    @ApiResponse(responseCode = "201", description = "Monitoramento criado.",
            headers = @Header(name = "Location", description = "Endereço do monitoramento criado.",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "400", ref = RespostaDeErro.REF_PARAMETRO_INVALIDO)
    @ApiResponse(responseCode = "404", ref = RespostaDeErro.REF_NAO_ENCONTRADO)
    @ApiResponse(responseCode = "409", ref = RespostaDeErro.REF_EM_CONFLITO)
    @ApiResponse(responseCode = "422", ref = RespostaDeErro.REF_REGRA_VIOLADA)
    @ApiResponse(responseCode = "503", ref = RespostaDeErro.REF_SERVICO_INDISPONIVEL)
    ResponseEntity<MonitoramentoResponse> criar(@Parameter(hidden = true) UsuarioAutenticado usuario,
                                                CriarMonitoramentoRequest pedido);

    @Operation(operationId = "listarMonitoramentos", summary = "Monitoramentos do usuário",
            description = "Ordem fixa, do mais recente ao mais antigo; `sort` não é aceito.")
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Página, a partir de 0.",
            schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0"))
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Itens por página; acima de 20 vira 20.",
            schema = @Schema(type = "integer", format = "int32", minimum = "1", maximum = "20", defaultValue = "20"))
    @ApiResponse(responseCode = "200", description = "Página de monitoramentos.")
    @ApiResponse(responseCode = "404", ref = RespostaDeErro.REF_NAO_ENCONTRADO)
    MonitoramentosResponse listar(@Parameter(hidden = true) UsuarioAutenticado usuario,
                                  @Parameter(hidden = true) Pageable paginacao);

    @Operation(operationId = "alterarMonitoramento", summary = "Pausa, reativa ou troca o alvo",
            description = "Ao menos um dos campos é obrigatório.")
    @ApiResponse(responseCode = "200", description = "Monitoramento alterado.")
    @ApiResponse(responseCode = "400", ref = RespostaDeErro.REF_PARAMETRO_INVALIDO)
    @ApiResponse(responseCode = "404", ref = RespostaDeErro.REF_NAO_ENCONTRADO)
    @ApiResponse(responseCode = "422", ref = RespostaDeErro.REF_REGRA_VIOLADA)
    MonitoramentoResponse alterar(@Parameter(hidden = true) UsuarioAutenticado usuario, Long id,
                                  AlterarMonitoramentoRequest pedido);

    @Operation(operationId = "apagarMonitoramento", summary = "Apaga o monitoramento")
    @ApiResponse(responseCode = "204", description = "Monitoramento apagado.")
    @ApiResponse(responseCode = "400", ref = RespostaDeErro.REF_PARAMETRO_INVALIDO)
    @ApiResponse(responseCode = "404", ref = RespostaDeErro.REF_NAO_ENCONTRADO)
    ResponseEntity<Void> apagar(@Parameter(hidden = true) UsuarioAutenticado usuario, Long id);
}
