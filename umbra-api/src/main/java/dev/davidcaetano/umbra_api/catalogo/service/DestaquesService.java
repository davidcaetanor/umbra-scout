package dev.davidcaetano.umbra_api.catalogo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.davidcaetano.umbra_api.catalogo.dto.response.DestaquesResponse;
import dev.davidcaetano.umbra_api.catalogo.dto.response.ProdutoResumoResponse;
import dev.davidcaetano.umbra_api.catalogo.properties.OfertasProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DestaquesService {

    private static final String CHAVE_UNICA = "destaques";

    private final ProdutoService produtoService;
    private final OfertasProperties properties;
    private final Cache<String, DestaquesResponse> cache;

    public DestaquesService(ProdutoService produtoService, OfertasProperties properties) {
        this.produtoService = produtoService;
        this.properties = properties;
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

        Set<String> jogosVistos = new HashSet<>();
        List<ProdutoResumoResponse> umaEdicaoPorJogo = candidatos.stream()
                .filter(produto -> jogosVistos.add(ChaveDoJogo.de(produto.nome())))
                .limit(properties.destaques())
                .toList();
        return new DestaquesResponse(umaEdicaoPorJogo);
    }
}
