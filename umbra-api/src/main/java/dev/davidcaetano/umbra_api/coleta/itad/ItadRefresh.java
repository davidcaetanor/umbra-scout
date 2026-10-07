package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaComUltimoPrecoProjecao;
import dev.davidcaetano.umbra_api.catalogo.repository.PrecoRepository;
import dev.davidcaetano.umbra_api.coleta.Cobertura;
import dev.davidcaetano.umbra_api.coleta.FiltroMoeda;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadOfertaPrecoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class ItadRefresh {

    private static final OrigemColeta FONTE = OrigemColeta.ITAD_API;
    private static final int TAMANHO_LOTE = 200;

    private final ItadClient itadClient;
    private final PrecoRepository precoRepository;

    public ResultadoColeta atualizar() {
        Set<CodigoLoja> lojasDoItad = LojaItad.codigosLoja();
        Map<ProdutoNaLoja, OfertaComUltimoPrecoProjecao> conhecidas = carregarOfertasConhecidas(lojasDoItad);

        Set<String> gidsPedidos = new HashSet<>();
        conhecidas.keySet().forEach(produtoNaLoja -> gidsPedidos.add(produtoNaLoja.chaveItad()));

        if (gidsPedidos.isEmpty()) {
            log.info("Refresh ITAD sem jogo com chave ITAD no catalogo; nenhuma chamada feita");
            return new ResultadoColeta(FONTE, List.of(), 0, 0, new Reconciliacao(null, 0, 0), Cobertura.amostra());
        }

        List<ItadPrecoJogoResponse> precos = buscarPrecosEmLotes(gidsPedidos);

        List<OfertaColetada> ofertas = new ArrayList<>();
        FiltroMoeda filtroMoeda = new FiltroMoeda();
        Set<String> gidsDevolvidos = new HashSet<>();
        Set<UUID> gidsDistintosNaResposta = new HashSet<>();

        int totalElegivel = 0;
        int totalSemPreco = 0;
        int paresSemOfertaConhecida = 0;

        for (ItadPrecoJogoResponse precoJogo : precos) {
            gidsDistintosNaResposta.add(precoJogo.id());
            String chaveItad = precoJogo.id().toString();

            if (!gidsPedidos.contains(chaveItad)) {
                log.debug("Refresh ITAD recebeu gid nao pedido, ignorado: gid={}", chaveItad);
                continue;
            }
            gidsDevolvidos.add(chaveItad);

            for (ItadOfertaPrecoResponse deal : MenorPrecoPorLoja.escolher(precoJogo.deals())) {
                Optional<CodigoLoja> loja = deal.shop() == null ? Optional.empty()
                        : LojaItad.codigoLojaPorShopId(deal.shop().id());

                if (deal.price() == null || loja.isEmpty()) {
                    log.warn("Refresh ITAD com oferta sem valor ou com shop ausente ou nao esperado: gid={} shop={}",
                            chaveItad, deal.shop());
                    totalElegivel++;
                    totalSemPreco++;
                    continue;
                }

                String moedaPreco = deal.price().currency();
                String moedaRegular = deal.regular() == null ? null : deal.regular().currency();

                if (!filtroMoeda.aceita(moedaPreco, moedaRegular)) {
                    log.debug("Refresh ITAD com oferta fora da moeda esperada: gid={} shop={} precoCurrency={} regularCurrency={}",
                            chaveItad, deal.shop(), moedaPreco, moedaRegular);
                    filtroMoeda.registrarDescarte(moedaPreco, moedaRegular);
                    continue;
                }

                OfertaComUltimoPrecoProjecao conhecida = conhecidas.get(new ProdutoNaLoja(chaveItad, loja.get()));

                if (conhecida == null) {
                    paresSemOfertaConhecida++;
                    continue;
                }

                totalElegivel++;
                ofertas.add(toOfertaColetada(loja.get(), conhecida, deal));
            }
        }

        filtroMoeda.logarResumo(FONTE);

        log.info("Refresh ITAD: gidsPedidos={} gidsDevolvidos={} naoVoltaram={} ofertasMontadas={} paresSemOfertaConhecida={}",
                gidsPedidos.size(), gidsDevolvidos.size(), gidsPedidos.size() - gidsDevolvidos.size(),
                ofertas.size(), paresSemOfertaConhecida);

        return new ResultadoColeta(FONTE, ofertas, totalElegivel, totalSemPreco,
                new Reconciliacao(null, precos.size(), gidsDistintosNaResposta.size()),
                Cobertura.censoDosProdutos(lojasDoItad, gidsPedidos));
    }

    private Map<ProdutoNaLoja, OfertaComUltimoPrecoProjecao> carregarOfertasConhecidas(Set<CodigoLoja> lojas) {
        Map<ProdutoNaLoja, OfertaComUltimoPrecoProjecao> conhecidas = new HashMap<>();

        precoRepository.findOfertasAtivasComUltimoPrecoPorCodigoLojaIn(lojas.stream().map(CodigoLoja::name).toList())
                .stream()
                .filter(oferta -> oferta.getChaveItad() != null)
                .forEach(oferta -> conhecidas.merge(
                        new ProdutoNaLoja(oferta.getChaveItad(), CodigoLoja.valueOf(oferta.getCodigoLoja())),
                        oferta,
                        (atual, outra) -> atual.getOfertaId() <= outra.getOfertaId() ? atual : outra));

        return conhecidas;
    }

    private List<ItadPrecoJogoResponse> buscarPrecosEmLotes(Set<String> gidsPedidos) {
        List<UUID> gids = gidsPedidos.stream().map(UUID::fromString).toList();
        List<ItadPrecoJogoResponse> precos = new ArrayList<>();

        for (int inicio = 0; inicio < gids.size(); inicio += TAMANHO_LOTE) {
            precos.addAll(itadClient.buscarPrecos(gids.subList(inicio, Math.min(inicio + TAMANHO_LOTE, gids.size()))));
        }

        return precos;
    }

    private static OfertaColetada toOfertaColetada(CodigoLoja loja, OfertaComUltimoPrecoProjecao conhecida,
                                                   ItadOfertaPrecoResponse deal) {
        return new OfertaColetada(
                loja,
                conhecida.getIdentificadorLoja(),
                TipoProduto.JOGO,
                conhecida.getNomeProduto(),
                null,
                deal.url(),
                null,
                conhecida.getChaveItad(),
                deal.price().amountInt(),
                (long) deal.regular().amountInt(),
                (short) deal.cut(),
                true,
                FONTE,
                deal.expiry()
        );
    }

    private record ProdutoNaLoja(String chaveItad, CodigoLoja loja) {
    }
}
