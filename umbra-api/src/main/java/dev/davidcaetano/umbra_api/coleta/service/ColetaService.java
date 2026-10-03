package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.catalogo.entity.LojaEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.OfertaEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.PrecoEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.repository.LojaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.PrecoRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.PrecoRepository.UltimoPrecoProjecao;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoRepository;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.service.PreparadorDeRodada.GrupoChave;
import dev.davidcaetano.umbra_api.coleta.service.PreparadorDeRodada.GrupoDeOfertas;
import dev.davidcaetano.umbra_api.coleta.service.RegraDeGravacaoDePreco.UltimoPreco;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ColetaService {

    private static final int TAMANHO_BLOCO_CONSULTA = 1000;

    private final ProdutoRepository produtoRepository;
    private final OfertaRepository ofertaRepository;
    private final PrecoRepository precoRepository;
    private final LojaRepository lojaRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final PreparadorDeRodada preparadorDeRodada = new PreparadorDeRodada();
    private final RegraDeGravacaoDePreco regraDeGravacaoDePreco = new RegraDeGravacaoDePreco();
    private final JulgamentoDeRodada julgamentoDeRodada = new JulgamentoDeRodada();

    public ColetaService(ProdutoRepository produtoRepository,
                          OfertaRepository ofertaRepository,
                          PrecoRepository precoRepository,
                          LojaRepository lojaRepository,
                          Clock clock,
                          TransactionTemplate transactionTemplate) {

        this.produtoRepository = produtoRepository;
        this.ofertaRepository = ofertaRepository;
        this.precoRepository = precoRepository;
        this.lojaRepository = lojaRepository;
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    public VereditoRodada gravar(ResultadoColeta resultado) {

        VereditoRodada veredito = julgamentoDeRodada.julgar(resultado);

        if (veredito.situacao() != SituacaoRodada.SAUDAVEL) {
            Reconciliacao reconciliacao = resultado.reconciliacao();
            log.warn("Alarme de schema drift: fonte={} situacao={} condicoes={} elegiveis={} semPreco={} "
                            + "declarado={} brutos={} distintos={}",
                    resultado.fonte(), veredito.situacao(), veredito.condicoes(), resultado.totalElegivel(),
                    resultado.totalSemPreco(), reconciliacao.declarado(), reconciliacao.brutos(),
                    reconciliacao.distintos());
        }

        if (veredito.situacao() == SituacaoRodada.REJEITADA) {
            return veredito;
        }

        List<List<GrupoDeOfertas>> chunks = preparadorDeRodada.preparar(resultado.ofertas());

        Map<CodigoLoja, LojaEntity> lojasPorCodigo = lojaRepository.findAll().stream()
                .collect(Collectors.toMap(LojaEntity::getCodigo, Function.identity()));

        OffsetDateTime agora = OffsetDateTime.now(clock);
        Contadores totais = Contadores.ZERO;
        int chunksComFalha = 0;
        RuntimeException ultimaFalha = null;

        for (List<GrupoDeOfertas> chunk : chunks) {
            try {
                totais = totais.somar(transactionTemplate.execute(status -> gravarChunk(chunk, lojasPorCodigo, agora)));
            } catch (RuntimeException falhaDoChunk) {
                chunksComFalha++;
                ultimaFalha = falhaDoChunk;
                log.warn("Falha ao gravar chunk com {} grupo(s) de oferta; chunk descartado, "
                        + "preços já gravados em chunks anteriores são mantidos", chunk.size(), falhaDoChunk);
            }
        }

        if (!chunks.isEmpty() && chunksComFalha == chunks.size()) {
            log.error("Nenhum dos {} chunk(s) desta rodada foi gravado", chunks.size(), ultimaFalha);
            throw new IllegalStateException(
                    "Nenhum dos " + chunks.size() + " chunk(s) desta rodada foi gravado", ultimaFalha);
        }

        if (chunksComFalha > 0) {
            log.info("Coleta gravada: fonte={}, {} produto(s) criado(s), {} oferta(s) criada(s), "
                            + "{} preço(s) gravado(s) ({} de {} chunk(s) descartado(s))",
                    resultado.fonte(), totais.produtosCriados(), totais.ofertasCriadas(), totais.precosGravados(),
                    chunksComFalha, chunks.size());
        } else {
            log.info("Coleta gravada: fonte={}, {} produto(s) criado(s), {} oferta(s) criada(s), {} preço(s) gravado(s)",
                    resultado.fonte(), totais.produtosCriados(), totais.ofertasCriadas(), totais.precosGravados());
        }

        return veredito;
    }

    private Contadores gravarChunk(List<GrupoDeOfertas> grupos, Map<CodigoLoja, LojaEntity> lojasPorCodigo, OffsetDateTime agora) {

        ContextoDoChunk contexto = carregarContexto(grupos, lojasPorCodigo);

        int produtosCriados = 0;
        int ofertasCriadas = 0;
        int precosGravados = 0;

        for (GrupoDeOfertas grupo : grupos) {
            List<OfertaColetada> ofertasDoGrupo = grupo.ofertas();

            ResolucaoProduto resolucao = resolverProduto(grupo.chave(), ofertasDoGrupo, contexto, agora);
            if (resolucao.criado()) {
                produtosCriados++;
            }

            for (OfertaColetada ofertaColetada : ofertasDoGrupo) {
                LojaEntity loja = contexto.lojaPorCodigo(ofertaColetada.loja());
                ChaveOferta chaveOferta = new ChaveOferta(loja.getId(), ofertaColetada.identificadorLoja());

                OfertaEntity oferta = contexto.ofertasExistentes().get(chaveOferta);
                boolean ofertaNova = oferta == null;
                if (ofertaNova) {
                    oferta = criarOferta(resolucao.produto(), loja, ofertaColetada, agora);
                    contexto.ofertasExistentes().put(chaveOferta, oferta);
                    ofertasCriadas++;
                }

                if (devePersistirPreco(ofertaNova, oferta.getId(), ofertaColetada, contexto, agora)) {
                    precoRepository.save(PrecoEntity.novo(oferta, ofertaColetada.valorCentavos(),
                            ofertaColetada.valorOriginalCentavos(), ofertaColetada.descontoPct(),
                            ofertaColetada.disponivel(), ofertaColetada.origemColeta(),
                            ofertaColetada.expiry(), agora));
                    precosGravados++;
                }
            }
        }

        return new Contadores(produtosCriados, ofertasCriadas, precosGravados);
    }

    private ResolucaoProduto resolverProduto(GrupoChave chave, List<OfertaColetada> ofertasDoGrupo,
                                              ContextoDoChunk contexto, OffsetDateTime agora) {

        ProdutoEntity.DadosProduto dados = dadosDoPrimeiro(ofertasDoGrupo);

        if (chave.chaveItad() != null) {
            ProdutoEntity produtoPorChaveItad = contexto.produtosPorChaveItad().get(chave.chaveItad());
            if (produtoPorChaveItad != null) {
                produtoPorChaveItad.completarDadosAusentes(dados, agora);
                return new ResolucaoProduto(produtoPorChaveItad, false);
            }
        }

        for (OfertaColetada ofertaColetada : ofertasDoGrupo) {
            LojaEntity loja = contexto.lojaPorCodigo(ofertaColetada.loja());
            OfertaEntity ofertaExistente = contexto.ofertasExistentes().get(new ChaveOferta(loja.getId(), ofertaColetada.identificadorLoja()));
            if (ofertaExistente != null) {
                ProdutoEntity produtoDaOferta = ofertaExistente.getProduto();
                produtoDaOferta.completarDadosAusentes(dados, agora);
                return new ResolucaoProduto(produtoDaOferta, false);
            }
        }

        ProdutoEntity produtoNovo = produtoRepository.save(ProdutoEntity.novo(ofertasDoGrupo.get(0).tipo(), dados, agora));
        return new ResolucaoProduto(produtoNovo, true);
    }

    private OfertaEntity criarOferta(ProdutoEntity produto, LojaEntity loja, OfertaColetada ofertaColetada, OffsetDateTime agora) {
        return ofertaRepository.save(
                OfertaEntity.nova(produto, loja, ofertaColetada.identificadorLoja(), ofertaColetada.url(), agora));
    }

    private boolean devePersistirPreco(boolean ofertaNova, Long ofertaId, OfertaColetada ofertaColetada,
                                        ContextoDoChunk contexto, OffsetDateTime agora) {

        UltimoPrecoProjecao ultimoPreco = contexto.ultimoPrecoPorOferta().get(ofertaId);
        UltimoPreco ultimoPrecoConhecido = ultimoPreco == null ? null
                : new UltimoPreco(ultimoPreco.getValorCentavos(), ultimoPreco.isDisponivel(), ultimoPreco.getColetadoEm());

        return regraDeGravacaoDePreco.devePersistir(ofertaNova, ultimoPrecoConhecido,
                ofertaColetada.valorCentavos(), ofertaColetada.disponivel(), agora.toInstant());
    }

    private ContextoDoChunk carregarContexto(List<GrupoDeOfertas> grupos, Map<CodigoLoja, LojaEntity> lojasPorCodigo) {

        List<OfertaColetada> todasAsOfertas = grupos.stream().flatMap(grupo -> grupo.ofertas().stream()).toList();

        Map<Short, List<String>> identificadoresPorLoja = todasAsOfertas.stream()
                .collect(Collectors.groupingBy(
                        oferta -> lojaObrigatoria(lojasPorCodigo, oferta.loja()).getId(),
                        Collectors.mapping(OfertaColetada::identificadorLoja, Collectors.toList())));

        Map<ChaveOferta, OfertaEntity> ofertasExistentes = carregarOfertasExistentes(identificadoresPorLoja);

        Set<String> chavesItad = grupos.stream()
                .map(grupo -> grupo.chave().chaveItad())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, ProdutoEntity> produtosPorChaveItad = carregarProdutosPorChaveItad(chavesItad);

        List<Long> idsDasOfertasExistentes = ofertasExistentes.values().stream().map(OfertaEntity::getId).toList();
        Map<Long, UltimoPrecoProjecao> ultimoPrecoPorOferta = carregarUltimoPreco(idsDasOfertasExistentes);

        return new ContextoDoChunk(lojasPorCodigo, ofertasExistentes, produtosPorChaveItad, ultimoPrecoPorOferta);
    }

    private Map<ChaveOferta, OfertaEntity> carregarOfertasExistentes(Map<Short, List<String>> identificadoresPorLoja) {

        Map<ChaveOferta, OfertaEntity> ofertasExistentes = new HashMap<>();

        for (Map.Entry<Short, List<String>> identificadoresDaLoja : identificadoresPorLoja.entrySet()) {
            for (List<String> bloco : emBlocos(identificadoresDaLoja.getValue(), TAMANHO_BLOCO_CONSULTA)) {
                for (OfertaEntity oferta : ofertaRepository.findByLojaIdAndIdentificadorLojaIn(identificadoresDaLoja.getKey(), bloco)) {
                    ofertasExistentes.put(new ChaveOferta(identificadoresDaLoja.getKey(), oferta.getIdentificadorLoja()), oferta);
                }
            }
        }

        return ofertasExistentes;
    }

    private Map<String, ProdutoEntity> carregarProdutosPorChaveItad(Set<String> chavesItad) {

        Map<String, ProdutoEntity> produtosPorChaveItad = new HashMap<>();

        for (List<String> bloco : emBlocos(chavesItad, TAMANHO_BLOCO_CONSULTA)) {
            for (ProdutoEntity produto : produtoRepository.findByChaveItadIn(bloco)) {
                produtosPorChaveItad.put(produto.getChaveItad(), produto);
            }
        }

        return produtosPorChaveItad;
    }

    private Map<Long, UltimoPrecoProjecao> carregarUltimoPreco(List<Long> idsDasOfertas) {

        Map<Long, UltimoPrecoProjecao> ultimoPrecoPorOferta = new HashMap<>();

        for (List<Long> bloco : emBlocos(idsDasOfertas, TAMANHO_BLOCO_CONSULTA)) {
            for (UltimoPrecoProjecao projecao : precoRepository.findUltimoPrecoPorOfertaIdIn(bloco)) {
                ultimoPrecoPorOferta.put(projecao.getOfertaId(), projecao);
            }
        }

        return ultimoPrecoPorOferta;
    }

    private static LojaEntity lojaObrigatoria(Map<CodigoLoja, LojaEntity> lojasPorCodigo, CodigoLoja codigo) {
        LojaEntity loja = lojasPorCodigo.get(codigo);
        if (loja == null) {
            throw new IllegalStateException("Loja " + codigo + " não está semeada na tabela loja");
        }
        return loja;
    }

    private static ProdutoEntity.DadosProduto dadosDoPrimeiro(List<OfertaColetada> ofertasDoGrupo) {
        OfertaColetada primeira = ofertasDoGrupo.get(0);
        return new ProdutoEntity.DadosProduto(primeira.nome(), primeira.categoria(), primeira.imagemUrl(), primeira.chaveItad());
    }

    private static <T> List<List<T>> emBlocos(Collection<T> itens, int tamanhoDoBloco) {
        List<T> lista = new ArrayList<>(itens);
        List<List<T>> blocos = new ArrayList<>();

        for (int inicio = 0; inicio < lista.size(); inicio += tamanhoDoBloco) {
            blocos.add(lista.subList(inicio, Math.min(inicio + tamanhoDoBloco, lista.size())));
        }

        return blocos;
    }

    private record ContextoDoChunk(Map<CodigoLoja, LojaEntity> lojasPorCodigo,
                                    Map<ChaveOferta, OfertaEntity> ofertasExistentes,
                                    Map<String, ProdutoEntity> produtosPorChaveItad,
                                    Map<Long, UltimoPrecoProjecao> ultimoPrecoPorOferta) {

        LojaEntity lojaPorCodigo(CodigoLoja codigo) {
            return lojaObrigatoria(lojasPorCodigo, codigo);
        }
    }

    private record ChaveOferta(Short lojaId, String identificadorLoja) {
    }

    private record ResolucaoProduto(ProdutoEntity produto, boolean criado) {
    }

    private record Contadores(int produtosCriados, int ofertasCriadas, int precosGravados) {

        static final Contadores ZERO = new Contadores(0, 0, 0);

        Contadores somar(Contadores outro) {
            return new Contadores(
                    produtosCriados + outro.produtosCriados,
                    ofertasCriadas + outro.ofertasCriadas,
                    precosGravados + outro.precosGravados);
        }
    }
}
