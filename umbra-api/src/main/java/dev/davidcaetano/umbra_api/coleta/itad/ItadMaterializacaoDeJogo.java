package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.JogoExternoNaoEncontradoException;
import dev.davidcaetano.umbra_api.catalogo.JogoExternoRecusadoException;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.service.MaterializacaoDeJogo;
import dev.davidcaetano.umbra_api.catalogo.service.MaterializacaoIndisponivelException;
import dev.davidcaetano.umbra_api.coleta.FiltroMoeda;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoInfoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadOfertaPrecoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;
import dev.davidcaetano.umbra_api.coleta.service.ColetaService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class ItadMaterializacaoDeJogo implements MaterializacaoDeJogo {

    private static final String TIPO_JOGO = "game";
    private static final String TETO_DE_INCLUSOES = "itad-materializacao";

    private final ItadBuscaClient itadBuscaClient;
    private final ColetaService coletaService;
    private final RateLimiter tetoDeInclusoes;

    public ItadMaterializacaoDeJogo(ItadBuscaClient itadBuscaClient, ColetaService coletaService,
                                    RateLimiterRegistry rateLimiterRegistry) {
        this.itadBuscaClient = itadBuscaClient;
        this.coletaService = coletaService;
        this.tetoDeInclusoes = rateLimiterRegistry.rateLimiter(TETO_DE_INCLUSOES);
    }

    @Override
    public void materializar(String gid) {
        if (!tetoDeInclusoes.acquirePermission()) {
            log.warn("Teto de inclusoes de jogo externo atingido; inclusao recusada: gid={}", gid);
            throw new MaterializacaoIndisponivelException("teto de inclusoes de jogo externo atingido");
        }

        try {
            coletaService.gravarSobDemanda(montarOfertas(gid, UUID.fromString(gid)));
        } catch (ItadApiException | CallNotPermittedException | RequestNotPermitted indisponivel) {
            throw new MaterializacaoIndisponivelException("ITAD indisponivel para incluir o jogo: "
                    + indisponivel.getMessage(), indisponivel);
        }
    }

    private List<OfertaColetada> montarOfertas(String gid, UUID id) {
        ItadJogoInfoResponse info = itadBuscaClient.buscarInfo(id)
                .orElseThrow(() -> new JogoExternoNaoEncontradoException(gid));
        if (!TIPO_JOGO.equals(info.type())) {
            throw JogoExternoRecusadoException.naoEJogo(gid);
        }

        List<ItadOfertaPrecoResponse> deals = itadBuscaClient.buscarPrecos(List.of(id)).stream()
                .filter(preco -> id.equals(preco.id()))
                .findFirst()
                .map(ItadPrecoJogoResponse::deals)
                .orElse(List.of());

        String imagemUrl = info.assets() == null ? null : info.assets().boxart();
        FiltroMoeda filtroMoeda = new FiltroMoeda();
        List<OfertaColetada> ofertas = new ArrayList<>();

        for (ItadOfertaPrecoResponse deal : MenorPrecoPorLoja.escolher(deals)) {
            if (deal.shop() == null || deal.price() == null || deal.regular() == null
                    || !filtroMoeda.aceita(deal.price().currency(), deal.regular().currency())) {
                continue;
            }
            Optional<CodigoLoja> loja = LojaItad.codigoLojaPorShopId(deal.shop().id());
            if (loja.isEmpty()) {
                continue;
            }
            identificadorNativo(deal.shop().id(), id).ifPresent(identificador -> ofertas.add(
                    ConversaoDeOfertaItad.paraOfertaColetada(loja.get(), identificador, info.title(), imagemUrl,
                            id, deal)));
        }

        if (ofertas.isEmpty()) {
            throw JogoExternoRecusadoException.semOfertaNasLojasAcompanhadas(gid);
        }
        return ofertas;
    }

    private Optional<String> identificadorNativo(int shopId, UUID id) {
        List<String> identificadores = itadBuscaClient.resolverIdentificadorNativo(shopId, List.of(id))
                .getOrDefault(id, List.of());
        return identificadores == null ? Optional.empty() : identificadores.stream()
                .sorted()
                .findFirst()
                .map(identificadorItad -> LojaItad.identificadorNativo(shopId, identificadorItad));
    }
}
