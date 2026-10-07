package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.entity.OfertaEntity;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ResolucaoDeGidFaltante {

    private static final LojaItad LOJA = LojaItad.STEAM;
    private static final int TAMANHO_LOTE = 200;

    private final ItadClient itadClient;
    private final OfertaRepository ofertaRepository;
    private final ProdutoRepository produtoRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public ResolucaoDeGidFaltante(ItadClient itadClient,
                                  OfertaRepository ofertaRepository,
                                  ProdutoRepository produtoRepository,
                                  TransactionTemplate transactionTemplate,
                                  Clock clock) {

        this.itadClient = itadClient;
        this.ofertaRepository = ofertaRepository;
        this.produtoRepository = produtoRepository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public int resolver() {
        List<OfertaEntity> ofertasSemGid =
                ofertaRepository.findAtivasDeProdutoAtivoSemChaveItad(CodigoLoja.STEAM, TipoProduto.JOGO);

        long produtosSemGid = ofertasSemGid.stream().map(oferta -> oferta.getProduto().getId()).distinct().count();

        if (ofertasSemGid.isEmpty()) {
            log.info("Resolucao de gid faltante: produtosSemGid=0, ITAD nao consultado");
            return 0;
        }

        Map<String, UUID> gidPorIdentificadorItad = consultarItad(ofertasSemGid);

        Map<String, ProdutoEntity> donoPorGid = donosPorGid(gidPorIdentificadorItad.values());

        Map<String, ProdutoEntity> carimbadoNestaExecucaoPorGid = new HashMap<>();
        Map<Long, String> gidACarimbarPorProduto = new LinkedHashMap<>();
        int semResposta = 0;
        int conflitos = 0;

        for (OfertaEntity oferta : ofertasSemGid) {
            ProdutoEntity produto = oferta.getProduto();
            UUID gidDevolvido = gidPorIdentificadorItad.get(identificadorItad(oferta));

            if (gidDevolvido == null) {
                semResposta++;
                continue;
            }

            if (gidACarimbarPorProduto.containsKey(produto.getId())) {
                continue;
            }

            String gid = gidDevolvido.toString();
            ProdutoEntity dono = donoPorGid.getOrDefault(gid, carimbadoNestaExecucaoPorGid.get(gid));

            if (dono != null && !dono.getId().equals(produto.getId())) {
                log.warn("Gid descoberto ja pertence a outro produto, nao carimbado: appid={} gid={} "
                                + "produtoSemGid={} ({}) produtoDono={} ({})",
                        oferta.getIdentificadorLoja(), gid, produto.getId(), produto.getNome(),
                        dono.getId(), dono.getNome());
                conflitos++;
                continue;
            }

            carimbadoNestaExecucaoPorGid.put(gid, produto);
            gidACarimbarPorProduto.put(produto.getId(), gid);
        }

        carimbar(gidACarimbarPorProduto);

        log.info("Resolucao de gid faltante: produtosSemGid={} carimbados={} semRespostaDoItad={} conflitos={}",
                produtosSemGid, gidACarimbarPorProduto.size(), semResposta, conflitos);

        return gidACarimbarPorProduto.size();
    }

    private Map<String, UUID> consultarItad(List<OfertaEntity> ofertasSemGid) {
        List<String> identificadores = List.copyOf(ofertasSemGid.stream()
                .map(ResolucaoDeGidFaltante::identificadorItad)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        Map<String, UUID> gidPorIdentificadorItad = new HashMap<>();
        for (int inicio = 0; inicio < identificadores.size(); inicio += TAMANHO_LOTE) {
            List<String> lote = identificadores.subList(inicio, Math.min(inicio + TAMANHO_LOTE, identificadores.size()));
            gidPorIdentificadorItad.putAll(itadClient.resolverGidPorIdentificador(LOJA.shopId(), lote));
        }

        return gidPorIdentificadorItad;
    }

    private Map<String, ProdutoEntity> donosPorGid(Collection<UUID> gidsDevolvidos) {
        Set<String> gids = gidsDevolvidos.stream()
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .collect(Collectors.toSet());

        if (gids.isEmpty()) {
            return Map.of();
        }

        return produtoRepository.findByChaveItadIn(gids).stream()
                .collect(Collectors.toMap(ProdutoEntity::getChaveItad, Function.identity()));
    }

    private void carimbar(Map<Long, String> gidACarimbarPorProduto) {
        if (gidACarimbarPorProduto.isEmpty()) {
            return;
        }

        OffsetDateTime agora = OffsetDateTime.now(clock);

        transactionTemplate.executeWithoutResult(status -> gidACarimbarPorProduto.forEach((produtoId, gid) ->
                produtoRepository.findById(produtoId).orElseThrow()
                        .completarDadosAusentes(new ProdutoEntity.DadosProduto(null, null, null, gid), agora)));
    }

    private static String identificadorItad(OfertaEntity oferta) {
        return LojaItad.identificadorItad(LOJA.shopId(), oferta.getIdentificadorLoja());
    }
}
