package dev.davidcaetano.umbra_api.monitoramento.controller;

import dev.davidcaetano.umbra_api.monitoramento.dto.request.AlterarMonitoramentoRequest;
import dev.davidcaetano.umbra_api.monitoramento.dto.request.CriarMonitoramentoRequest;
import dev.davidcaetano.umbra_api.monitoramento.dto.request.FiltroDeMonitoramentosRequest;
import dev.davidcaetano.umbra_api.monitoramento.dto.response.MonitoramentoResponse;
import dev.davidcaetano.umbra_api.monitoramento.dto.response.MonitoramentosResponse;
import dev.davidcaetano.umbra_api.monitoramento.service.MonitoramentoService;
import dev.davidcaetano.umbra_api.usuario.service.UsuarioAutenticado;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/monitoramentos")
@RequiredArgsConstructor
public class MonitoramentoController implements MonitoramentoApi {

    private final MonitoramentoService monitoramentoService;

    @Override
    @PostMapping
    public ResponseEntity<MonitoramentoResponse> criar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                       @Valid @RequestBody CriarMonitoramentoRequest pedido) {
        MonitoramentoResponse criado = monitoramentoService.criar(usuario, pedido);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                        .path("/{id}").buildAndExpand(criado.id()).toUri())
                .body(criado);
    }

    @Override
    @GetMapping
    public MonitoramentosResponse listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                         @Valid @ModelAttribute FiltroDeMonitoramentosRequest filtro,
                                         Pageable paginacao) {
        return monitoramentoService.listar(usuario, filtro.produtoId(), paginacao);
    }

    @Override
    @PatchMapping("/{id}")
    public MonitoramentoResponse alterar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                                         @Valid @RequestBody AlterarMonitoramentoRequest pedido) {
        return monitoramentoService.alterar(usuario, id, pedido);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        monitoramentoService.apagar(usuario, id);
        return ResponseEntity.noContent().build();
    }
}
