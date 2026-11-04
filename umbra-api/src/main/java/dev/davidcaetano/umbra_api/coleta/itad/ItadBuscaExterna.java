package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.service.BuscaExterna;
import dev.davidcaetano.umbra_api.catalogo.service.BuscaExternaIndisponivelException;
import dev.davidcaetano.umbra_api.catalogo.service.JogoExterno;
import dev.davidcaetano.umbra_api.catalogo.service.OfertaExterna;
import dev.davidcaetano.umbra_api.coleta.FiltroMoeda;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoBuscaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadOfertaPrecoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ItadBuscaExterna implements BuscaExterna {

    private static final String TIPO_JOGO = "game";
    private static final List<Integer> ORDEM_DAS_LOJAS = LojaItad.shopIds();

    private final ItadBuscaClient itadBuscaClient;

    @Override
    public List<JogoExterno> buscar(String termo) {
        try {
            return buscarJogosComOferta(termo);
        } catch (ItadApiException | CallNotPermittedException | RequestNotPermitted ex) {
            throw new BuscaExternaIndisponivelException("ITAD indisponível para a busca externa: " + ex.getMessage(), ex);
        }
    }

    private List<JogoExterno> buscarJogosComOferta(String termo) {
        List<ItadJogoBuscaResponse> jogos = itadBuscaClient.buscarJogos(termo).stream()
                .filter(jogo -> TIPO_JOGO.equals(jogo.type()))
                .toList();

        if (jogos.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<ItadOfertaPrecoResponse>> ofertasPorGid = itadBuscaClient
                .buscarPrecos(jogos.stream().map(ItadJogoBuscaResponse::id).toList())
                .stream()
                .collect(Collectors.toMap(ItadPrecoJogoResponse::id, ItadPrecoJogoResponse::deals,
                        (primeira, repetida) -> primeira));

        FiltroMoeda filtroMoeda = new FiltroMoeda();
        return jogos.stream()
                .map(jogo -> paraJogoExterno(jogo, ofertasPorGid.getOrDefault(jogo.id(), List.of()), filtroMoeda))
                .filter(jogo -> !jogo.ofertas().isEmpty())
                .toList();
    }

    private static JogoExterno paraJogoExterno(ItadJogoBuscaResponse jogo,
                                               List<ItadOfertaPrecoResponse> deals,
                                               FiltroMoeda filtroMoeda) {
        List<OfertaExterna> ofertas = MenorPrecoPorLoja.escolher(deals).stream()
                .filter(deal -> deal.shop() != null && deal.price() != null && deal.regular() != null)
                .filter(deal -> filtroMoeda.aceita(deal.price().currency(), deal.regular().currency()))
                .sorted(Comparator.comparingInt(deal -> ORDEM_DAS_LOJAS.indexOf(deal.shop().id())))
                .flatMap(deal -> LojaItad.codigoLojaPorShopId(deal.shop().id())
                        .map(loja -> paraOfertaExterna(loja, deal))
                        .stream())
                .toList();

        String imagemUrl = jogo.assets() == null ? null : jogo.assets().boxart();
        return new JogoExterno(jogo.id().toString(), jogo.title(), imagemUrl, ofertas);
    }

    private static OfertaExterna paraOfertaExterna(CodigoLoja loja, ItadOfertaPrecoResponse deal) {
        return new OfertaExterna(
                loja,
                deal.url(),
                deal.price().amountInt(),
                (long) deal.regular().amountInt(),
                (short) deal.cut(),
                deal.expiry() == null ? null : deal.expiry().toInstant());
    }
}
