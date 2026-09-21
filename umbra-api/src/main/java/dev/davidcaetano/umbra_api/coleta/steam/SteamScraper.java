package dev.davidcaetano.umbra_api.coleta.steam;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.coleta.Cobertura;
import dev.davidcaetano.umbra_api.coleta.FiltroMoeda;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.Scraper;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamAppDetalhesResponse;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamJogoDescobertoResponse;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamPrecoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SteamScraper implements Scraper {

    private final SteamClient steamClient;

    @Override
    public OrigemColeta fonte() {
        return OrigemColeta.STEAM_API;
    }

    @Override
    public Cobertura cobertura() {
        return Cobertura.AMOSTRA;
    }

    @Override
    public ResultadoColeta coletar() {
        List<SteamJogoDescobertoResponse> descobertos = steamClient.buscarDescoberta().specials().items();

        Set<Integer> appidsVistos = new LinkedHashSet<>();
        for (SteamJogoDescobertoResponse item : descobertos) {
            appidsVistos.add(item.id());
        }

        List<OfertaColetada> resultado = new ArrayList<>();
        FiltroMoeda filtroMoeda = new FiltroMoeda();

        int totalElegivel = 0;
        int totalSemPreco = 0;

        for (SteamJogoDescobertoResponse item : descobertos) {
            if (!item.discounted()) {
                log.debug("Item Steam sem desconto: id={} name={}", item.id(), item.name());
                continue;
            }

            SteamAppDetalhesResponse detalhes = steamClient.buscarDetalhes(item.id());

            TriagemItem triagem = triar(item, detalhes, filtroMoeda);

            if (triagem == TriagemItem.FORA_ESCOPO) {
                continue;
            }

            totalElegivel++;

            if (triagem == TriagemItem.SEM_PRECO) {
                totalSemPreco++;
                continue;
            }

            resultado.add(toOfertaColetada(item, detalhes.data().overview()));
        }

        filtroMoeda.logarResumo(fonte());

        return new ResultadoColeta(resultado, totalElegivel, totalSemPreco,
                new Reconciliacao(null, descobertos.size(), appidsVistos.size()));
    }

    private static OfertaColetada toOfertaColetada(SteamJogoDescobertoResponse item, SteamPrecoResponse overview) {
        return new OfertaColetada(
                CodigoLoja.STEAM,
                String.valueOf(item.id()),
                TipoProduto.JOGO,
                item.name(),
                null,
                "https://store.steampowered.com/app/" + item.id() + "/",
                item.headerImage(),
                null,
                overview.finalPrice(),
                (long) overview.initial(),
                (short) overview.discountPercent(),
                true,
                OrigemColeta.STEAM_API,
                Instant.ofEpochSecond(item.discountExpiration()).atOffset(ZoneOffset.UTC)
        );
    }

    private static TriagemItem triar(SteamJogoDescobertoResponse item,
                                     SteamAppDetalhesResponse detalhes,
                                     FiltroMoeda filtroMoeda) {

        if (detalhes == null || !detalhes.success() || detalhes.data() == null) {
            log.warn("Item Steam com detalhes ausentes: id={} detalhes={}", item.id(), detalhes);
            return TriagemItem.SEM_PRECO;
        }

        if (!"game".equals(detalhes.data().type())) {
            log.debug("Item Steam fora do escopo: id={} type={}", item.id(), detalhes.data().type());
            return TriagemItem.FORA_ESCOPO;
        }

        SteamPrecoResponse overview = detalhes.data().overview();

        if (overview == null) {
            log.warn("Item Steam sem bloco de preco: id={}", item.id());
            return TriagemItem.SEM_PRECO;
        }

        if (!filtroMoeda.aceita(overview.currency())) {
            log.debug("Item Steam fora da moeda esperada: id={} currency={}", item.id(), overview.currency());
            filtroMoeda.registrarDescarte(overview.currency());
            return TriagemItem.FORA_ESCOPO;
        }

        return TriagemItem.ACEITO;
    }

    private enum TriagemItem {
        ACEITO,
        FORA_ESCOPO,
        SEM_PRECO
    }
}
