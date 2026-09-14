package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.coleta.FiltroMoeda;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.Scraper;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadDescobertaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoDescobertoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadLojaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadOfertaPrecoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItadScraper implements Scraper {

    // Orçamento de descoberta por rodada — decisão de produto sobre quanto catálogo semear
    // a cada execução, não guarda de segurança. Também é o teto de memória de uma rodada:
    // PAGINAS_POR_RODADA * limite da página itens ficam em memória de uma vez.
    private static final int PAGINAS_POR_RODADA = 30;
    private static final int TAMANHO_LOTE = 200;

    private final ItadClient itadClient;

    @Override
    public OrigemColeta fonte() {
        return OrigemColeta.ITAD_API;
    }

    @Override
    public ResultadoColeta coletar() {
        List<ItadJogoDescobertoResponse> descobertos = buscarTodaDescoberta();

        List<ItadJogoDescobertoResponse> jogos = descobertos.stream()
                .filter(jogo -> "game".equals(jogo.type()))
                .toList();

        Map<UUID, TituloImagem> tituloImagemPorGid = new HashMap<>();
        for (ItadJogoDescobertoResponse jogo : jogos) {
            tituloImagemPorGid.putIfAbsent(jogo.id(),
                    new TituloImagem(jogo.title(), jogo.assets() == null ? null : jogo.assets().boxart()));
        }

        List<UUID> gidsDistintos = jogos.stream()
                .map(ItadJogoDescobertoResponse::id)
                .distinct()
                .toList();

        List<ItadPrecoJogoResponse> precos = buscarTodosPrecos(gidsDistintos);

        Map<Integer, Set<UUID>> gidsPorShop = montarGidsPorShop(precos);
        Map<Integer, Map<UUID, String>> identificadorPorShop = resolverIdentificadoresNativos(gidsPorShop);

        List<OfertaColetada> resultado = new ArrayList<>();
        FiltroMoeda filtroMoeda = new FiltroMoeda();

        int totalElegivel = 0;
        int totalSemPreco = 0;

        for (ItadPrecoJogoResponse precoJogo : precos) {
            UUID gid = precoJogo.id();
            TituloImagem info = tituloImagemPorGid.get(gid);

            for (ItadOfertaPrecoResponse deal : precoJogo.deals()) {
                String moedaPreco = deal.price() == null ? null : deal.price().currency();
                String moedaRegular = deal.regular() == null ? null : deal.regular().currency();

                if (!filtroMoeda.aceita(moedaPreco, moedaRegular)) {
                    log.debug("Oferta ITAD fora da moeda esperada: gid={} shop={} precoCurrency={} regularCurrency={}",
                            gid, deal.shop(), moedaPreco, moedaRegular);
                    filtroMoeda.registrarDescarte(moedaPreco, moedaRegular);
                    continue;
                }

                totalElegivel++;

                Optional<CodigoLoja> loja = resolverLoja(deal.shop());

                if (loja.isEmpty()) {
                    log.warn("Oferta ITAD com shop ausente ou nao esperado na resposta de precos: gid={} shop={}",
                            gid, deal.shop());
                    totalSemPreco++;
                    continue;
                }

                Map<UUID, String> identificadoresShop = identificadorPorShop.get(deal.shop().id());
                String identificadorLoja = identificadoresShop == null ? null : identificadoresShop.get(gid);

                if (identificadorLoja == null || info == null) {
                    log.warn("Oferta ITAD com dado inconsistente entre chamadas: gid={} shopId={} identificadorNativo={} tituloImagem={}",
                            gid, deal.shop().id(), identificadorLoja, info);
                    totalSemPreco++;
                    continue;
                }

                resultado.add(new OfertaColetada(
                        loja.get(),
                        identificadorLoja,
                        TipoProduto.JOGO,
                        info.titulo(),
                        null,
                        deal.url(),
                        info.imagemUrl(),
                        gid.toString(),
                        deal.price().amountInt(),
                        (long) deal.regular().amountInt(),
                        (short) deal.cut(),
                        true,
                        OrigemColeta.ITAD_API,
                        deal.expiry()
                ));
            }
        }

        filtroMoeda.logarResumo(fonte());

        return new ResultadoColeta(resultado, totalElegivel, totalSemPreco);
    }

    private List<ItadJogoDescobertoResponse> buscarTodaDescoberta() {
        List<ItadJogoDescobertoResponse> todos = new ArrayList<>();

        int offset = 0;
        int pagina = 0;

        while (true) {
            if (pagina >= PAGINAS_POR_RODADA) {
                log.warn("Descoberta ITAD atingiu o orcamento de {} paginas da rodada, parando com hasMore ainda verdadeiro (offset={})",
                        PAGINAS_POR_RODADA, offset);
                break;
            }

            ItadDescobertaResponse resposta = itadClient.buscarDescoberta(offset);
            todos.addAll(resposta.list());
            pagina++;

            if (!resposta.hasMore()) {
                break;
            }
            offset = resposta.nextOffset();
        }

        return todos;
    }

    private static Map<Integer, Set<UUID>> montarGidsPorShop(List<ItadPrecoJogoResponse> precos) {
        Map<Integer, Set<UUID>> gidsPorShop = new HashMap<>();

        for (ItadPrecoJogoResponse precoJogo : precos) {
            for (ItadOfertaPrecoResponse deal : precoJogo.deals()) {
                ItadLojaResponse shop = deal.shop();
                if (shop == null || LojaItad.codigoLojaPorShopId(shop.id()).isEmpty()) {
                    continue;
                }
                gidsPorShop.computeIfAbsent(shop.id(), k -> new LinkedHashSet<>()).add(precoJogo.id());
            }
        }

        return gidsPorShop;
    }

    private Map<Integer, Map<UUID, String>> resolverIdentificadoresNativos(Map<Integer, Set<UUID>> gidsPorShop) {
        Map<Integer, Map<UUID, String>> identificadorPorShop = new HashMap<>();

        for (Map.Entry<Integer, Set<UUID>> entry : gidsPorShop.entrySet()) {
            int shopId = entry.getKey();
            List<UUID> gids = List.copyOf(entry.getValue());

            Map<UUID, String> primeiroIdentificador = new HashMap<>();

            for (List<UUID> lote : particionar(gids, TAMANHO_LOTE)) {
                Map<UUID, List<String>> resolvido = itadClient.resolverIdentificadorNativo(shopId, lote);
                resolvido.forEach((gid, identificadores) -> {
                    if (identificadores != null && !identificadores.isEmpty()) {
                        primeiroIdentificador.put(gid, primeiroIdentificadorOrdenado(identificadores));
                    }
                });
            }

            identificadorPorShop.put(shopId, primeiroIdentificador);
        }

        return identificadorPorShop;
    }

    private static String primeiroIdentificadorOrdenado(List<String> identificadores) {
        return identificadores.stream().sorted().findFirst().orElseThrow();
    }

    private List<ItadPrecoJogoResponse> buscarTodosPrecos(List<UUID> gidsDistintos) {
        List<ItadPrecoJogoResponse> precos = new ArrayList<>();

        for (List<UUID> lote : particionar(gidsDistintos, TAMANHO_LOTE)) {
            precos.addAll(itadClient.buscarPrecos(lote));
        }

        return precos;
    }

    private static <T> List<List<T>> particionar(List<T> lista, int tamanho) {
        List<List<T>> lotes = new ArrayList<>();
        for (int i = 0; i < lista.size(); i += tamanho) {
            lotes.add(lista.subList(i, Math.min(i + tamanho, lista.size())));
        }
        return lotes;
    }

    private static Optional<CodigoLoja> resolverLoja(ItadLojaResponse shop) {
        if (shop == null) {
            return Optional.empty();
        }

        return LojaItad.codigoLojaPorShopId(shop.id());
    }

    private record TituloImagem(String titulo, String imagemUrl) {
    }
}