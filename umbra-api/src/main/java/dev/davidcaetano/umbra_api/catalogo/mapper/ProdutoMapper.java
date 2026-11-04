package dev.davidcaetano.umbra_api.catalogo.mapper;

import dev.davidcaetano.umbra_api.catalogo.dto.response.MinimoHistoricoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.OfertaResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.PontoHistoricoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.ProdutoExternoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.ProdutoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.ProdutoResumoResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.SerieLojaResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.VariacaoResponse;
import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaAtualProjecao;
import dev.davidcaetano.umbra_api.catalogo.repository.PontoDePrecoProjecao;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoResumoProjecao;
import dev.davidcaetano.umbra_api.catalogo.service.DegrauDePreco;
import dev.davidcaetano.umbra_api.catalogo.service.JogoExterno;
import dev.davidcaetano.umbra_api.catalogo.service.OfertaExterna;
import dev.davidcaetano.umbra_api.catalogo.service.VariacaoDePreco;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;

@Mapper
public interface ProdutoMapper {

    ProdutoResumoResponse paraResumo(ProdutoResumoProjecao produto, OfertaResponse melhorPreco);

    OfertaResponse paraOferta(ProdutoResumoProjecao melhorOferta, boolean gratuito, boolean promocaoEncerrada);

    OfertaResponse paraOferta(OfertaAtualProjecao oferta, boolean gratuito, boolean promocaoEncerrada);

    OfertaResponse paraOferta(OfertaExterna oferta, String loja, Instant coletadoEm, boolean gratuito,
                              boolean promocaoEncerrada);

    ProdutoExternoResponse paraExterno(JogoExterno jogo, OfertaResponse melhorPreco);

    ProdutoResponse paraProduto(ProdutoEntity produto, OfertaResponse melhorPreco, List<OfertaResponse> ofertas);

    List<PontoHistoricoResponse> paraPontos(List<DegrauDePreco> degraus);

    SerieLojaResponse paraSerieLoja(String loja, List<DegrauDePreco> pontos);

    @Mapping(target = "em", source = "ponto.coletadoEm")
    MinimoHistoricoResponse paraMinimo(PontoDePrecoProjecao ponto, boolean gratuito);

    VariacaoResponse paraVariacao(VariacaoDePreco variacao);
}
