package dev.davidcaetano.umbra_api.catalogo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.davidcaetano.umbra_api.catalogo.dto.response.DestaquesResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.ProdutoResumoResponse;
import dev.davidcaetano.umbra_api.catalogo.properties.OfertasProperties;
import dev.davidcaetano.umbra_api.catalogo.repository.QualidadeDoCandidatoProjecao;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DestaquesService {

    private static final String CHAVE_UNICA = "destaques";

    private final ProdutoService produtoService;
    private final ConsultaDoCatalogo consultaDoCatalogo;
    private final OfertasProperties properties;
    private final CuradoriaDosDestaques curadoria;
    private final Cache<String, DestaquesResponse> cache;

    public DestaquesService(ProdutoService produtoService, ConsultaDoCatalogo consultaDoCatalogo,
                            OfertasProperties properties) {
        this.produtoService = produtoService;
        this.consultaDoCatalogo = consultaDoCatalogo;
        this.properties = properties;
        this.curadoria = new CuradoriaDosDestaques(properties.destaques(),
                properties.qualidade().destaquesPorPublicador());
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(properties.validadeDosDestaques())
                .maximumSize(1)
                .build();
    }

    public DestaquesResponse destaques() {
        return cache.get(CHAVE_UNICA, chave -> calcular());
    }

    private DestaquesResponse calcular() {
        List<ProdutoResumoResponse> candidatos = produtoService.buscar(FiltroDoCatalogo.dosDestaques(),
                        OrdenacaoCatalogo.MELHORES_OFERTAS, false,
                        PageRequest.of(0, properties.candidatosDosDestaques()), null)
                .conteudo();

        Map<Long, QualidadeDoCandidatoProjecao> qualidade = consultaDoCatalogo
                .qualidadeDosCandidatos(candidatos.stream().map(ProdutoResumoResponse::id).toList()).stream()
                .collect(Collectors.toMap(QualidadeDoCandidatoProjecao::getProdutoId, Function.identity()));

        List<CandidatoADestaque> emOrdem = candidatos.stream()
                .map(produto -> paraCandidato(produto, qualidade.get(produto.id())))
                .toList();
        Map<Long, ProdutoResumoResponse> porId = candidatos.stream()
                .collect(Collectors.toMap(ProdutoResumoResponse::id, Function.identity()));

        return new DestaquesResponse(curadoria.escolher(emOrdem).stream().map(porId::get).toList());
    }

    private static CandidatoADestaque paraCandidato(ProdutoResumoResponse produto,
                                                    QualidadeDoCandidatoProjecao qualidade) {
        return new CandidatoADestaque(produto.id(), ChaveDoJogo.de(produto.nome()),
                qualidade == null ? null : qualidade.getPublicador(),
                qualidade != null && qualidade.isQualificado());
    }
}
