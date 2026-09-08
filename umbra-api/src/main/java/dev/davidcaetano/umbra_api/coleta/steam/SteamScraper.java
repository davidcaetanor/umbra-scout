package dev.davidcaetano.umbra_api.coleta.steam;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.coleta.ProdutoColetado;
import dev.davidcaetano.umbra_api.coleta.Scraper;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamAppDetalhesResponse;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamJogoDescobertoResponse;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamPrecoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SteamScraper implements Scraper {

    private static final String MOEDA_ESPERADA = "BRL";

    private final SteamClient steamClient;

    @Override
    public OrigemColeta fonte() {
        return OrigemColeta.STEAM_API;
    }

    @Override
    public List<ProdutoColetado> coletar() {
        List<SteamJogoDescobertoResponse> descobertos = steamClient.buscarDescoberta().specials().items();

        List<ProdutoColetado> resultado = new ArrayList<>();

        for (SteamJogoDescobertoResponse item : descobertos) {
            if (!item.discounted()) {
                log.warn("Descartando item Steam sem desconto: id={} name={}", item.id(), item.name());
                continue;
            }

            SteamAppDetalhesResponse detalhes = steamClient.buscarDetalhes(item.id());

            if (!isJogoValido(item, detalhes)) {
                continue;
            }

            resultado.add(toProdutoColetado(item, detalhes.data().overview()));
        }

        return resultado;
    }

    private boolean isJogoValido(SteamJogoDescobertoResponse item, SteamAppDetalhesResponse detalhes) {
        if (detalhes == null || !detalhes.success() || detalhes.data() == null) {
            log.warn("Descartando item Steam com detalhes ausentes ou inconsistentes: id={} detalhes={}",
                    item.id(), detalhes);
            return false;
        }

        if (!"game".equals(detalhes.data().type())) {
            log.warn("Descartando item Steam que nao e do tipo game: id={} type={}",
                    item.id(), detalhes.data().type());
            return false;
        }

        if (!MOEDA_ESPERADA.equals(detalhes.data().overview().currency())) {
            log.warn("Descartando item Steam fora da moeda esperada: id={} currency={}",
                    item.id(), detalhes.data().overview().currency());
            return false;
        }

        return true;
    }

    private static ProdutoColetado toProdutoColetado(SteamJogoDescobertoResponse item, SteamPrecoResponse overview) {
        return new ProdutoColetado(
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
                OffsetDateTime.now(),
                Instant.ofEpochSecond(item.discountExpiration()).atOffset(ZoneOffset.UTC)
        );
    }
}
