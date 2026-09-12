package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.coleta.ProdutoColetado;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.Scraper;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadDescobertaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoDescobertoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadLojaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadOfertaDescobertaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadOfertaPrecoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadValorResponse;
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
import java.util.TreeSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItadScraper implements Scraper {

    private static final List<Integer> SHOPS = List.of(61, 50);
    private static final int MAX_PAGINAS_DESCOBERTA = 50;
    private static final int TAMANHO_LOTE_PRECOS = 200;
    private static final String MOEDA_ESPERADA = "BRL";
    private static final String MOEDA_AUSENTE = "ausente";

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
        Set<String> moedasDescartadas = new TreeSet<>();

        int totalElegivel = 0;
        int totalSemPreco = 0;
        int descartadosPorMoeda = 0;

        for (ItadPrecoJogoResponse precoJogo : precos) {
            UUID gid = precoJogo.id();
            TituloImagem info = tituloImagemPorGid.get(gid);

            for (ItadOfertaPrecoResponse deal : precoJogo.deals()) {

                if (!ehMoedaEsperada(deal)) {
                    log.debug("Oferta ITAD fora da moeda esperada: gid={} shop={} precoCurrency={} regularCurrency={}",
                            gid, deal.shop(), moedaDe(deal.price()), moedaDe(deal.regular()));
                    moedasDescartadas.add(moedaDe(deal.price()));
                    moedasDescartadas.add(moedaDe(deal.regular()));
                    descartadosPorMoeda++;
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
                    log.warn("Oferta ITAD com dado inconsistente entre chamadas: " +
                                    "gid={} shopId={} identificadorNativo={} tituloImagem={}",
                            gid, deal.shop().id(), identificadorLoja, info);
                    totalSemPreco++;
                    continue;
                }

                resultado.add(new ProdutoColetado(
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

        if (descartadosPorMoeda > 0) {
            log.warn("ITAD: {} ofertas descartadas por moedas diferentes de {} nesta rodada (moedas vistas: {})",
                    descartadosPorMoeda, MOEDA_ESPERADA, moedasDescartadas);
        }

        return new ResultadoColeta(resultado, totalElegivel, totalSemPreco);
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

    private static boolean ehMoedaEsperada(ItadOfertaPrecoResponse deal) {
        return deal.price() != null
                && deal.regular() != null
                && MOEDA_ESPERADA.equals(deal.price().currency())
                && MOEDA_ESPERADA.equals(deal.regular().currency());
    }

    private static String moedaDe(ItadValorResponse valor) {
        return valor == null || valor.currency() == null ? MOEDA_AUSENTE : valor.currency();
    }

    private static Optional<CodigoLoja> resolverLoja(ItadLojaResponse shop) {
        if (shop == null) {
            return Optional.empty();
        }

        return switch (shop.id()) {
            case 61 -> Optional.of(CodigoLoja.STEAM);
            case 50 -> Optional.of(CodigoLoja.NUUVEM);
            default -> Optional.empty();
        };
    }

    private record TituloImagem(String titulo, String imagemUrl) {
    }
}