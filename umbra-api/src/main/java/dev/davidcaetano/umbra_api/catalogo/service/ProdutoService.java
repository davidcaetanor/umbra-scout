package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.ProdutoNaoEncontradoException;
import dev.davidcaetano.umbra_api.catalogo.dto.response.BuscaProdutosResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.HistoricoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.MinimoHistoricoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.OfertaResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.ProdutoExternoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.ProdutoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.ProdutoResumoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.SerieLojaResponse;
import dev.davidcaetano.umbra_api.catalogo.entity.LojaEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CategoriaHardware;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.mapper.ProdutoMapper;
import dev.davidcaetano.umbra_api.catalogo.properties.BuscaProperties;
import dev.davidcaetano.umbra_api.catalogo.repository.LojaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaAtualProjecao;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.PontoDePrecoProjecao;
import dev.davidcaetano.umbra_api.catalogo.repository.PrecoRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoResumoProjecao;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private static final String TERMO_QUE_CASA_TUDO = "";

    private final ProdutoRepository produtoRepository;
    private final OfertaRepository ofertaRepository;
    private final PrecoRepository precoRepository;
    private final LojaRepository lojaRepository;
    private final BuscaExternaService buscaExternaService;
    private final BuscaProperties buscaProperties;
    private final ProdutoMapper produtoMapper;
    private final Clock clock;

    public BuscaProdutosResponse buscar(String termo,
                                        CategoriaHardware categoria,
                                        OrdenacaoCatalogo ordenacao,
                                        boolean incluirExterno,
                                        Pageable paginacao) {
        Instant agora = clock.instant();
        Pageable semOrdenacaoDoCliente = PageRequest.of(paginacao.getPageNumber(), paginacao.getPageSize());

        Page<ProdutoResumoProjecao> locais = buscarNoCatalogo(termo, categoria, ordenacao, semOrdenacaoDoCliente);
        List<ProdutoResumoResponse> conteudo = locais.getContent().stream()
                .map(produto -> produtoMapper.paraResumo(produto, melhorPrecoDo(produto, agora)))
                .toList();
        List<ProdutoExternoResponse> externos = incluirExterno && semOrdenacaoDoCliente.getPageNumber() == 0
                ? buscarForaDoCatalogo(termo, agora)
                : List.of();

        return new BuscaProdutosResponse(conteudo, locais.getNumber(), locais.getSize(),
                locais.getTotalElements(), locais.getTotalPages(), externos);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse detalhar(Long id) {
        Instant agora = clock.instant();
        ProdutoEntity produto = buscarAtivo(id);

        List<OfertaResponse> ofertas = ofertaRepository.findDisponiveisComPrecoAtualPorProdutoId(id).stream()
                .map(oferta -> paraOferta(oferta, agora))
                .toList();
        OfertaResponse melhorPreco = ofertas.isEmpty() ? null : ofertas.getFirst();

        return produtoMapper.paraProduto(produto, melhorPreco, ofertas);
    }

    @Transactional(readOnly = true)
    public HistoricoResponse historico(Long id, PeriodoHistorico periodo) {
        Instant agora = clock.instant();
        buscarAtivo(id);

        Map<Long, List<DegrauDePreco>> degrausPorOferta = new TreeMap<>();
        Map<Long, String> lojaPorOferta = new HashMap<>();
        Optional<Instant> inicio = periodo.inicio(agora);

        inicio.ifPresentOrElse(
                inicioDaJanela -> {
                    precoRepository.findUltimoPontoDeCadaOfertaAntesDe(id, inicioDaJanela).forEach(entrada ->
                            acrescentar(degrausPorOferta, lojaPorOferta, entrada, inicioDaJanela));
                    precoRepository.findPontosDoProdutoDesde(id, inicioDaJanela).forEach(ponto ->
                            acrescentar(degrausPorOferta, lojaPorOferta, ponto, ponto.getColetadoEm()));
                },
                () -> precoRepository.findPontosDoProduto(id).forEach(ponto ->
                        acrescentar(degrausPorOferta, lojaPorOferta, ponto, ponto.getColetadoEm())));

        List<DegrauDePreco> melhorPreco = SerieDoMelhorPreco.calcular(degrausPorOferta);
        List<SerieLojaResponse> porLoja = degrausPorOferta.entrySet().stream()
                .map(oferta -> produtoMapper.paraSerieLoja(lojaPorOferta.get(oferta.getKey()), oferta.getValue()))
                .toList();

        MinimoHistoricoResponse minimo = precoRepository.findMinimoHistoricoDoProduto(id)
                .map(ponto -> produtoMapper.paraMinimo(ponto,
                        ClassificacaoDePreco.gratuito(ponto.getValorCentavos(), ponto.getValorOriginalCentavos())))
                .orElse(null);
        VariacaoDePreco variacao = IndicadoresDePreco.variacao(
                precoRepository.findPrecoAtualDoProduto(id).orElse(null),
                minimo == null ? null : minimo.valorCentavos());
        List<Long> faixas = IndicadoresDePreco.faixas(melhorPreco.stream()
                .map(DegrauDePreco::valorCentavos)
                .filter(Objects::nonNull)
                .toList());

        return new HistoricoResponse(periodo.toString(), agora,
                precoRepository.findPrimeiraObservacaoDoProduto(id).orElse(null),
                minimo, produtoMapper.paraVariacao(variacao), produtoMapper.paraPontos(melhorPreco), porLoja, faixas);
    }

    private static void acrescentar(Map<Long, List<DegrauDePreco>> degrausPorOferta,
                                    Map<Long, String> lojaPorOferta,
                                    PontoDePrecoProjecao ponto,
                                    Instant instante) {
        lojaPorOferta.putIfAbsent(ponto.getOfertaId(), ponto.getLoja());
        List<DegrauDePreco> degraus = degrausPorOferta.computeIfAbsent(ponto.getOfertaId(), oferta -> new ArrayList<>());
        DegrauDePreco degrau = new DegrauDePreco(instante, ponto.isDisponivel() ? ponto.getValorCentavos() : null);

        // As linhas chegam por instante e id: a segunda linha no mesmo instante é a de maior id, e fica.
        if (!degraus.isEmpty() && degraus.getLast().instante().equals(instante)) {
            degraus.set(degraus.size() - 1, degrau);
        } else {
            degraus.add(degrau);
        }
    }

    private List<ProdutoExternoResponse> buscarForaDoCatalogo(String termo, Instant agora) {
        List<JogoExterno> jogos = buscaExternaService.buscar(termo);
        if (jogos.isEmpty()) {
            return List.of();
        }

        Set<String> gidsNoCatalogo = produtoRepository.findByChaveItadIn(jogos.stream().map(JogoExterno::gid).toList())
                .stream()
                .map(ProdutoEntity::getChaveItad)
                .collect(Collectors.toSet());
        List<JogoExterno> foraDoCatalogo = jogos.stream()
                .filter(jogo -> !gidsNoCatalogo.contains(jogo.gid()))
                .limit(buscaProperties.externa().maximoResultados())
                .toList();

        Map<CodigoLoja, String> nomePorLoja = lojaRepository.findByCodigoIn(foraDoCatalogo.stream()
                        .flatMap(jogo -> jogo.ofertas().stream())
                        .map(OfertaExterna::codigoLoja)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(LojaEntity::getCodigo, LojaEntity::getNome));

        return foraDoCatalogo.stream()
                .map(jogo -> produtoMapper.paraExterno(jogo, melhorPrecoExterno(jogo, nomePorLoja, agora)))
                .toList();
    }

    private OfertaResponse melhorPrecoExterno(JogoExterno jogo, Map<CodigoLoja, String> nomePorLoja, Instant agora) {
        OfertaExterna melhor = jogo.ofertas().stream()
                .min(Comparator.comparingLong(OfertaExterna::valorCentavos))
                .orElseThrow();

        return produtoMapper.paraOferta(melhor, nomePorLoja.get(melhor.codigoLoja()), agora,
                ClassificacaoDePreco.gratuito(melhor.valorCentavos(), melhor.valorOriginalCentavos()),
                ClassificacaoDePreco.promocaoEncerrada(melhor.expiraEm(), agora));
    }

    private ProdutoEntity buscarAtivo(Long id) {
        return produtoRepository.findByIdAndAtivoTrue(id).orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    private Page<ProdutoResumoProjecao> buscarNoCatalogo(String termo,
                                                         CategoriaHardware categoria,
                                                         OrdenacaoCatalogo ordenacao,
                                                         Pageable paginacao) {
        String termoDaBusca = termo == null ? TERMO_QUE_CASA_TUDO : termo;
        String codigoCategoria = categoria == null ? null : categoria.name();

        return ordenacao == OrdenacaoCatalogo.MENOR_PRECO
                ? produtoRepository.buscarCatalogoPorMenorPreco(termoDaBusca, codigoCategoria, paginacao)
                : produtoRepository.buscarCatalogoPorNome(termoDaBusca, codigoCategoria, paginacao);
    }

    private OfertaResponse melhorPrecoDo(ProdutoResumoProjecao produto, Instant agora) {
        if (produto.getValorCentavos() == null) {
            return null;
        }

        return produtoMapper.paraOferta(produto,
                ClassificacaoDePreco.gratuito(produto.getValorCentavos(), produto.getValorOriginalCentavos()),
                ClassificacaoDePreco.promocaoEncerrada(produto.getExpiraEm(), agora));
    }

    private OfertaResponse paraOferta(OfertaAtualProjecao oferta, Instant agora) {
        return produtoMapper.paraOferta(oferta,
                ClassificacaoDePreco.gratuito(oferta.getValorCentavos(), oferta.getValorOriginalCentavos()),
                ClassificacaoDePreco.promocaoEncerrada(oferta.getExpiraEm(), agora));
    }
}
