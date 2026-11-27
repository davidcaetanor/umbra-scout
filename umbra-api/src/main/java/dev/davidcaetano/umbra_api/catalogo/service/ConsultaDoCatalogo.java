package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.CatalogoSobrecarregadoException;
import dev.davidcaetano.umbra_api.catalogo.properties.CatalogoProperties;
import dev.davidcaetano.umbra_api.catalogo.properties.OfertasProperties;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoResumoProjecao;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultaDoCatalogo {

    private final ProdutoRepository produtoRepository;
    private final OfertasProperties ofertasProperties;
    private final CatalogoProperties catalogoProperties;

    @Transactional(readOnly = true, timeoutString = "${umbra.catalogo.prazo-da-consulta-segundos}")
    public Page<ProdutoResumoProjecao> buscar(FiltroDoCatalogo filtro, OrdenacaoCatalogo ordenacao, Pageable paginacao) {
        try {
            return consultar(filtro, ordenacao, paginacao);
        } catch (QueryTimeoutException | TransactionTimedOutException prazoEstourado) {
            throw new CatalogoSobrecarregadoException(catalogoProperties.tentarDeNovoEm(), prazoEstourado);
        }
    }

    private Page<ProdutoResumoProjecao> consultar(FiltroDoCatalogo filtro, OrdenacaoCatalogo ordenacao, Pageable paginacao) {
        String codigoTipo = filtro.tipo() == null ? null : filtro.tipo().name();
        String codigoCategoria = filtro.categoria() == null ? null : filtro.categoria().name();
        String codigoOrdenacao = (ordenacao == null ? OrdenacaoCatalogo.NOME : ordenacao).toString();

        if (filtro.loja() == null) {
            return produtoRepository.buscarCatalogo(filtro.termo(), codigoTipo, codigoCategoria,
                    filtro.precoMinCentavos(), filtro.precoMaxCentavos(), filtro.descontoMin(), codigoOrdenacao,
                    ofertasProperties.regua(), paginacao);
        }

        return produtoRepository.buscarCatalogoDaLoja(filtro.loja().name(), filtro.termo(), codigoTipo,
                codigoCategoria, filtro.precoMinCentavos(), filtro.precoMaxCentavos(), filtro.descontoMin(),
                codigoOrdenacao, ofertasProperties.regua(), paginacao);
    }
}
