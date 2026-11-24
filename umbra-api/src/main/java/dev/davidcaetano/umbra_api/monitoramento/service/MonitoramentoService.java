package dev.davidcaetano.umbra_api.monitoramento.service;

import dev.davidcaetano.umbra_api.catalogo.JogoExternoRecusadoException;
import dev.davidcaetano.umbra_api.catalogo.ProdutoNaoEncontradoException;
import dev.davidcaetano.umbra_api.catalogo.dto.response.OfertaResponse;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaAtualProjecao;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoRepository;
import dev.davidcaetano.umbra_api.catalogo.enums.SituacaoDoPreco;
import dev.davidcaetano.umbra_api.catalogo.service.ClassificacaoDePreco;
import dev.davidcaetano.umbra_api.monitoramento.AlvoEmProdutoGratuitoException;
import dev.davidcaetano.umbra_api.monitoramento.MonitoramentoDuplicadoException;
import dev.davidcaetano.umbra_api.monitoramento.MonitoramentoNaoEncontradoException;
import dev.davidcaetano.umbra_api.monitoramento.dto.request.AlterarMonitoramentoRequest;
import dev.davidcaetano.umbra_api.monitoramento.dto.request.CriarMonitoramentoRequest;
import dev.davidcaetano.umbra_api.monitoramento.dto.response.MonitoramentoResponse;
import dev.davidcaetano.umbra_api.monitoramento.dto.response.MonitoramentosResponse;
import dev.davidcaetano.umbra_api.monitoramento.entity.MonitoramentoEntity;
import dev.davidcaetano.umbra_api.monitoramento.mapper.MonitoramentoMapper;
import dev.davidcaetano.umbra_api.monitoramento.properties.MonitoramentoProperties;
import dev.davidcaetano.umbra_api.monitoramento.repository.MonitoramentoRepository;
import dev.davidcaetano.umbra_api.monitoramento.repository.MonitoramentoResumoProjecao;
import dev.davidcaetano.umbra_api.usuario.UsuarioNaoEncontradoException;
import dev.davidcaetano.umbra_api.usuario.repository.UsuarioRepository;
import dev.davidcaetano.umbra_api.usuario.service.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonitoramentoService {

    static final String RESTRICAO_DE_UNICIDADE = "uq_monit_usuario_produto";

    private final MonitoramentoRepository monitoramentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;
    private final OfertaRepository ofertaRepository;
    private final MonitoramentoMapper monitoramentoMapper;
    private final MonitoramentoProperties monitoramentoProperties;
    private final InclusaoDeJogoExterno inclusaoDeJogoExterno;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public MonitoramentoResponse criar(UsuarioAutenticado usuario, CriarMonitoramentoRequest pedido) {
        exigirContaAtiva(usuario);
        Long produtoId = pedido.gid() == null ? pedido.produtoId() : produtoDoJogoExterno(pedido.gid().toString());
        return transactionTemplate.execute(status -> criarParaProduto(usuario, produtoId, pedido.precoAlvoCentavos()));
    }

    @Transactional(readOnly = true)
    public MonitoramentosResponse listar(UsuarioAutenticado usuario, Long produtoId, Pageable paginacao) {
        exigirContaAtiva(usuario);
        Instant agora = clock.instant();
        Page<MonitoramentoResumoProjecao> pagina = paginaDoUsuario(usuario.id(), produtoId,
                PageRequest.of(paginacao.getPageNumber(), paginacao.getPageSize()));

        List<MonitoramentoResponse> conteudo = pagina.getContent().stream()
                .map(resumo -> paraResponse(resumo, agora))
                .toList();

        return new MonitoramentosResponse(conteudo, pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }

    private Page<MonitoramentoResumoProjecao> paginaDoUsuario(Long usuarioId, Long produtoId, Pageable paginacao) {
        int diasParaDescontinuado = monitoramentoProperties.diasParaDescontinuado();
        return produtoId == null
                ? monitoramentoRepository.listarDoUsuario(usuarioId, diasParaDescontinuado, paginacao)
                : monitoramentoRepository.listarDoUsuarioPorProduto(usuarioId, produtoId, diasParaDescontinuado,
                        paginacao);
    }

    @Transactional
    public MonitoramentoResponse alterar(UsuarioAutenticado usuario, Long id, AlterarMonitoramentoRequest pedido) {
        exigirContaAtiva(usuario);
        MonitoramentoEntity monitoramento = monitoramentoRepository.findByIdAndUsuarioId(id, usuario.id())
                .orElseThrow(() -> new MonitoramentoNaoEncontradoException(id));
        OffsetDateTime agora = OffsetDateTime.now(clock);

        if (pedido.precoAlvoCentavos() != null) {
            recusarAlvoEmProdutoGratuito(monitoramento.getProdutoId());
            monitoramento.alterarAlvo(pedido.precoAlvoCentavos(), agora);
        }
        if (pedido.ativo() != null) {
            monitoramento.definirAtivo(pedido.ativo(), agora);
        }

        monitoramentoRepository.saveAndFlush(monitoramento);
        return resumoDoUsuario(id, usuario);
    }

    @Transactional
    public void apagar(UsuarioAutenticado usuario, Long id) {
        exigirContaAtiva(usuario);
        if (monitoramentoRepository.apagarDoUsuario(id, usuario.id()) == 0) {
            throw new MonitoramentoNaoEncontradoException(id);
        }
    }

    private Long produtoDoJogoExterno(String gid) {
        Optional<ProdutoEntity> existente = produtoRepository.findByChaveItad(gid);
        if (existente.isEmpty()) {
            inclusaoDeJogoExterno.incluir(gid);
            existente = produtoRepository.findByChaveItad(gid);
        }
        return existente.map(ProdutoEntity::getId)
                .orElseThrow(() -> JogoExternoRecusadoException.semOfertaNasLojasAcompanhadas(gid));
    }

    private MonitoramentoResponse criarParaProduto(UsuarioAutenticado usuario, Long produtoId, long precoAlvoCentavos) {
        produtoRepository.findByIdAndAtivoTrue(produtoId).orElseThrow(() -> new ProdutoNaoEncontradoException(produtoId));
        recusarAlvoEmProdutoGratuito(produtoId);

        if (monitoramentoRepository.existsByUsuarioIdAndProdutoId(usuario.id(), produtoId)) {
            throw new MonitoramentoDuplicadoException();
        }

        MonitoramentoEntity monitoramento;
        try {
            monitoramento = monitoramentoRepository.saveAndFlush(MonitoramentoEntity.novo(usuario.id(), produtoId,
                    precoAlvoCentavos, OffsetDateTime.now(clock)));
        } catch (DataIntegrityViolationException violacao) {
            if (violouAUnicidadeDoMonitoramento(violacao)) {
                throw new MonitoramentoDuplicadoException();
            }
            throw violacao;
        }

        return resumoDoUsuario(monitoramento.getId(), usuario);
    }

    private static boolean violouAUnicidadeDoMonitoramento(DataIntegrityViolationException violacao) {
        String mensagem = violacao.getMostSpecificCause().getMessage();
        return mensagem != null && mensagem.contains(RESTRICAO_DE_UNICIDADE);
    }

    private void exigirContaAtiva(UsuarioAutenticado usuario) {
        if (!usuarioRepository.existsByIdAndAtivoTrue(usuario.id())) {
            throw new UsuarioNaoEncontradoException();
        }
    }

    private void recusarAlvoEmProdutoGratuito(Long produtoId) {
        boolean melhorOfertaGratuita = ofertaRepository.findDisponiveisComPrecoAtualPorProdutoId(produtoId).stream()
                .min(Comparator.comparingLong(OfertaAtualProjecao::getValorCentavos))
                .map(melhor -> ClassificacaoDePreco.gratuito(melhor.getValorCentavos(), melhor.getValorOriginalCentavos()))
                .orElse(false);

        if (melhorOfertaGratuita) {
            throw new AlvoEmProdutoGratuitoException();
        }
    }

    private MonitoramentoResponse resumoDoUsuario(Long id, UsuarioAutenticado usuario) {
        return monitoramentoRepository.buscarResumoDoUsuario(id, usuario.id(),
                        monitoramentoProperties.diasParaDescontinuado())
                .map(resumo -> paraResponse(resumo, clock.instant()))
                .orElseThrow(() -> new MonitoramentoNaoEncontradoException(id));
    }

    private MonitoramentoResponse paraResponse(MonitoramentoResumoProjecao resumo, Instant agora) {
        if (resumo.getValorCentavos() == null) {
            return monitoramentoMapper.paraResponse(resumo, null, null, false);
        }

        long valor = resumo.getValorCentavos();
        SituacaoDoPreco situacao = ClassificacaoDePreco.situacao(resumo.getExpiraEm(), resumo.getVerificadoEm(), agora);
        OfertaResponse melhorPreco = monitoramentoMapper.paraOferta(resumo,
                ClassificacaoDePreco.gratuito(valor, resumo.getValorOriginalCentavos()), situacao);
        boolean alvoAtingido = situacao.valeParaAviso() && valor <= resumo.getPrecoAlvoCentavos();

        return monitoramentoMapper.paraResponse(resumo, melhorPreco, valor - resumo.getPrecoAlvoCentavos(),
                alvoAtingido);
    }
}
