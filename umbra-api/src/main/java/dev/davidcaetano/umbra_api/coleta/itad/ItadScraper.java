package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.coleta.ProdutoColetado;
import dev.davidcaetano.umbra_api.coleta.Scraper;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadDescobertaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoDescobertoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadOfertaDescobertaResponse;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItadScraper implements Scraper {

    private static final List<Integer> SHOPS = List.of(61, 50);
    private static final int MAX_PAGINAS_DESCOBERTA = 50;
    private static final int TAMANHO_LOTE_PRECOS = 200;
    private static final String MOEDA_ESPERADA = "BRL";

    private final ItadClient itadClient;

    @Override
    public OrigemColeta fonte() {
        return OrigemColeta.ITAD_API;
    }

    @Override
    public List<ProdutoColetado> coletar() {
        List<ItadJogoDescobertoResponse> descobertos = buscarTodaDescoberta();

        List<ItadJogoDescobertoResponse> jogos = descobertos.stream()
                .filter(jogo -> "game".equals(jogo.type()))
                .toList();

        Map<UUID, TituloImagem> tituloImagemPorGid = new HashMap<>();
        Map<Integer, Set<UUID>> gidsPorShop = new HashMap<>();

        for (ItadJogoDescobertoResponse jogo : jogos) {
            tituloImagemPorGid.putIfAbsent(jogo.id(),
                    new TituloImagem(jogo.title(), jogo.assets() == null ? null : jogo.assets().boxart()));

            ItadOfertaDescobertaResponse deal = jogo.deal();
            if (deal == null || deal.shop() == null) {
                continue;
            }
            gidsPorShop.computeIfAbsent(deal.shop().id(), k -> new LinkedHashSet<>()).add(jogo.id());
        }

        Map<Integer, Map<UUID, String>> identificadorPorShop = resolverIdentificadoresNativos(gidsPorShop);

        List<UUID> gidsDistintos = jogos.stream()
                .map(ItadJogoDescobertoResponse::id)
                .distinct()
                .toList();

        List<ItadPrecoJogoResponse> precos = buscarTodosPrecos(gidsDistintos);

        List<ProdutoColetado> resultado = new ArrayList<>();

        for (ItadPrecoJogoResponse precoJogo : precos) {
            UUID gid = precoJogo.id();
            TituloImagem info = tituloImagemPorGid.get(gid);

            for (ItadOfertaPrecoResponse deal : precoJogo.deals()) {
                int shopId = deal.shop().id();
                CodigoLoja loja = resolverLoja(shopId);

                Map<UUID, String> identificadoresDoShop = identificadorPorShop.get(shopId);
                String identificadorLoja = identificadoresDoShop == null ? null : identificadoresDoShop.get(gid);

                if (identificadorLoja == null || info == null) {
                    log.warn("Descartando oferta ITAD com dado inconsistente entre chamadas: gid={} shopId={} " +
                            "identificadorNativo={} tituloImagem={}", gid, shopId, identificadorLoja, info);
                    continue;
                }

                if (!MOEDA_ESPERADA.equals(deal.price().currency()) || !MOEDA_ESPERADA.equals(deal.regular().currency())) {
                    log.warn("Descartando oferta ITAD fora da moeda esperada: gid={} shopId={} precoCurrency={} " +
                            "regularCurrency={}", gid, shopId, deal.price().currency(), deal.regular().currency());
                    continue;
                }

                resultado.add(new ProdutoColetado(
                        loja,
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
                        deal.timestamp(),
                        deal.expiry()
                ));
            }
        }

        return resultado;
    }

    private List<ItadJogoDescobertoResponse> buscarTodaDescoberta() {
        List<ItadJogoDescobertoResponse> todos = new ArrayList<>();

        int offset = 0;
        int pagina = 0;

        while (true) {
            if (pagina >= MAX_PAGINAS_DESCOBERTA) {
                log.warn("Descoberta ITAD atingiu o limite de seguranca de {} paginas, parando com hasMore ainda " +
                        "verdadeiro (offset={})", MAX_PAGINAS_DESCOBERTA, offset);
                break;
            }

            ItadDescobertaResponse resposta = itadClient.buscarDescoberta(SHOPS, offset);
            todos.addAll(resposta.list());
            pagina++;

            if (!resposta.hasMore()) {
                break;
            }
            offset = resposta.nextOffset();
        }

        return todos;
    }

    private Map<Integer, Map<UUID, String>> resolverIdentificadoresNativos(Map<Integer, Set<UUID>> gidsPorShop) {
        Map<Integer, Map<UUID, String>> identificadorPorShop = new HashMap<>();

        for (Map.Entry<Integer, Set<UUID>> entry : gidsPorShop.entrySet()) {
            int shopId = entry.getKey();
            List<UUID> gids = List.copyOf(entry.getValue());

            if (gids.isEmpty()) {
                continue;
            }

            Map<UUID, List<String>> resolvido = itadClient.resolverIdentificadorNativo(shopId, gids);
            Map<UUID, String> primeiroIdentificador = new HashMap<>();

            resolvido.forEach((gid, identificadores) -> {
                if (identificadores != null && !identificadores.isEmpty()) {
                    // Escolha pragmática: quando há mais de um identificador nativo pro mesmo
                    // gid+shop (bundle com dois SKUs, ARCHITECTURE.md §4.8), pegamos o primeiro.
                    // Não é regra de negócio fechada — ponto em aberto aceito por ora.
                    primeiroIdentificador.put(gid, identificadores.getFirst());
                }
            });

            identificadorPorShop.put(shopId, primeiroIdentificador);
        }

        return identificadorPorShop;
    }

    private List<ItadPrecoJogoResponse> buscarTodosPrecos(List<UUID> gidsDistintos) {
        List<ItadPrecoJogoResponse> precos = new ArrayList<>();

        for (List<UUID> lote : particionar(gidsDistintos, TAMANHO_LOTE_PRECOS)) {
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

    private static CodigoLoja resolverLoja(int shopId) {
        return switch (shopId) {
            case 61 -> CodigoLoja.STEAM;
            case 50 -> CodigoLoja.NUUVEM;
            default -> throw new IllegalStateException(
                    "Shop id desconhecido vindo do ITAD (esperado 61=Steam ou 50=Nuuvem): " + shopId);
        };
    }

    private record TituloImagem(String titulo, String imagemUrl) {
    }
}
