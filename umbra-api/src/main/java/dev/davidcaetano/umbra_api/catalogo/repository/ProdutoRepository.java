package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<ProdutoEntity, Long> {

    String FILTRO_DA_LISTAGEM = """
             WHERE p.ativo
               AND (CAST(:categoria AS TEXT) IS NULL OR p.categoria = :categoria)
            """;

    String FILTRO_DO_CATALOGO = FILTRO_DA_LISTAGEM + """
               AND normalizar_busca(p.nome) LIKE '%' || normalizar_busca(:termo) || '%'
            """;

    String RESUMO_COM_MELHOR_OFERTA = """
            SELECT p.id, p.nome, p.tipo, p.categoria, p.imagem_url AS imagemUrl,
                   l.nome AS loja, m.url, m.valor_centavos AS valorCentavos,
                   m.valor_original_centavos AS valorOriginalCentavos, m.desconto_pct AS descontoPct,
                   m.expira_em AS expiraEm, m.coletado_em AS coletadoEm
              FROM produto p
              LEFT JOIN vw_melhor_oferta_atual m ON m.produto_id = p.id
              LEFT JOIN loja l ON l.id = m.loja_id
            """;

    String CONSULTA_DA_LISTAGEM = RESUMO_COM_MELHOR_OFERTA + FILTRO_DA_LISTAGEM;

    String CONTAGEM_DA_LISTAGEM = "SELECT count(*) FROM produto p" + FILTRO_DA_LISTAGEM;

    String CONSULTA_DO_CATALOGO = RESUMO_COM_MELHOR_OFERTA + FILTRO_DO_CATALOGO;

    String CONTAGEM_DO_CATALOGO = "SELECT count(*) FROM produto p" + FILTRO_DO_CATALOGO;

    String ORDEM_POR_NOME = " ORDER BY p.nome, p.id";

    String ORDEM_POR_MENOR_PRECO = " ORDER BY m.valor_centavos ASC NULLS LAST, p.id";

    List<ProdutoEntity> findByChaveItadIn(Collection<String> chavesItad);

    Optional<ProdutoEntity> findByIdAndAtivoTrue(Long id);

    @Query(value = CONSULTA_DA_LISTAGEM + ORDEM_POR_NOME,
            countQuery = CONTAGEM_DA_LISTAGEM, nativeQuery = true)
    Page<ProdutoResumoProjecao> listarCatalogoPorNome(@Param("categoria") String categoria,
                                                      Pageable paginacao);

    @Query(value = CONSULTA_DA_LISTAGEM + ORDEM_POR_MENOR_PRECO,
            countQuery = CONTAGEM_DA_LISTAGEM, nativeQuery = true)
    Page<ProdutoResumoProjecao> listarCatalogoPorMenorPreco(@Param("categoria") String categoria,
                                                            Pageable paginacao);

    @Query(value = CONSULTA_DO_CATALOGO + ORDEM_POR_NOME,
            countQuery = CONTAGEM_DO_CATALOGO, nativeQuery = true)
    Page<ProdutoResumoProjecao> buscarCatalogoPorNome(@Param("termo") String termo,
                                                      @Param("categoria") String categoria,
                                                      Pageable paginacao);

    @Query(value = CONSULTA_DO_CATALOGO + ORDEM_POR_MENOR_PRECO,
            countQuery = CONTAGEM_DO_CATALOGO, nativeQuery = true)
    Page<ProdutoResumoProjecao> buscarCatalogoPorMenorPreco(@Param("termo") String termo,
                                                            @Param("categoria") String categoria,
                                                            Pageable paginacao);
}
