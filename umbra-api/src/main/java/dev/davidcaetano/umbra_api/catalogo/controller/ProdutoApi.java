package dev.davidcaetano.umbra_api.catalogo.controller;

import dev.davidcaetano.umbra_api.catalogo.dto.request.BuscaProdutoRequest;
import dev.davidcaetano.umbra_api.catalogo.dto.response.BuscaProdutosResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.DestaquesResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.HistoricoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.ProdutoResponse;
import dev.davidcaetano.umbra_api.catalogo.service.PeriodoHistorico;
import dev.davidcaetano.umbra_api.comum.openapi.RespostaDeErro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;

@Tag(name = "Catálogo", description = "Busca, detalhe e histórico de preço, sem login.")
@SecurityRequirements
public interface ProdutoApi {

    @Operation(operationId = "buscarProdutos", summary = "Busca no catálogo",
            description = "Ordem fixa por `ordenacao`, sempre com `id` de desempate; `sort` não é aceito. "
                    + "Com `incluirExterno`, a primeira página traz em `externos` até 5 jogos achados no ITAD "
                    + "que ainda não estão no catálogo; nas demais páginas `externos` vem vazio. "
                    + "`categoria` vale só para hardware: com `tipo=JOGO` responde 400. "
                    + "`melhoresOfertas`, `maiorDesconto` e `descontoMin` valem só com `tipo=JOGO`; "
                    + "`melhoresOfertas` traz só o que tem desconto, pela economia em reais e pela "
                    + "proximidade do mínimo histórico. Com `loja`, preço e ordem são os da oferta nessa loja.")
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Página, a partir de 0.",
            schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0"))
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Itens por página; acima de 20 vira 20.",
            schema = @Schema(type = "integer", format = "int32", minimum = "1", maximum = "20", defaultValue = "20"))
    @ApiResponse(responseCode = "200", description = "Página do catálogo.")
    @ApiResponse(responseCode = "400", ref = RespostaDeErro.REF_PARAMETRO_INVALIDO)
    BuscaProdutosResponse buscar(@ParameterObject BuscaProdutoRequest busca,
                                 @Parameter(hidden = true) Pageable paginacao,
                                 @Parameter(hidden = true) HttpServletRequest requisicao);

    @Operation(operationId = "destaquesDoCatalogo", summary = "Destaques Umbra",
            description = "Jogos com a maior economia, pela mesma pontuação de `melhoresOfertas`, "
                    + "com uma edição por jogo. Recalculado no máximo a cada 10 minutos.")
    @ApiResponse(responseCode = "200", description = "Lista curta, já ordenada.")
    DestaquesResponse destaques();

    @Operation(operationId = "detalharProduto", summary = "Detalhe do produto com as ofertas atuais")
    @ApiResponse(responseCode = "200", description = "Produto ativo.")
    @ApiResponse(responseCode = "400", ref = RespostaDeErro.REF_PARAMETRO_INVALIDO)
    @ApiResponse(responseCode = "404", ref = RespostaDeErro.REF_NAO_ENCONTRADO)
    ProdutoResponse detalhar(Long id);

    @Operation(operationId = "historicoDoProduto", summary = "Histórico de preço do produto")
    @ApiResponse(responseCode = "200", description = "Série do melhor preço e série por loja no período.")
    @ApiResponse(responseCode = "400", ref = RespostaDeErro.REF_PARAMETRO_INVALIDO)
    @ApiResponse(responseCode = "404", ref = RespostaDeErro.REF_NAO_ENCONTRADO)
    HistoricoResponse historico(Long id, PeriodoHistorico periodo);
}
