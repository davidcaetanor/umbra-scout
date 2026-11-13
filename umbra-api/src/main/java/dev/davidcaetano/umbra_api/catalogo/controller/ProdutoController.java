package dev.davidcaetano.umbra_api.catalogo.controller;

import dev.davidcaetano.umbra_api.catalogo.dto.request.BuscaProdutoRequest;
import dev.davidcaetano.umbra_api.catalogo.dto.response.BuscaProdutosResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.HistoricoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.ProdutoResponse;
import dev.davidcaetano.umbra_api.catalogo.service.PeriodoHistorico;
import dev.davidcaetano.umbra_api.catalogo.service.ProdutoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @GetMapping
    public BuscaProdutosResponse buscar(@Valid @ModelAttribute BuscaProdutoRequest busca, Pageable paginacao,
                                        HttpServletRequest requisicao) {
        String ipDeOrigem = requisicao.getRemoteAddr();
        return produtoService.buscar(busca.nome(), busca.categoria(), busca.ordenacao(), busca.incluirExterno(),
                paginacao, ipDeOrigem);
    }

    @GetMapping("/{id}")
    public ProdutoResponse detalhar(@PathVariable Long id) {
        return produtoService.detalhar(id);
    }

    @GetMapping("/{id}/historico")
    public HistoricoResponse historico(@PathVariable Long id,
                                       @RequestParam(defaultValue = "90d") PeriodoHistorico periodo) {
        return produtoService.historico(id, periodo);
    }
}
