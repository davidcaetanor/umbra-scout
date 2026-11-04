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

    String FILTRO_DO_CATALOGO = """
             WHERE p.ativo
               AND normalizar_busca(p.nome) LIKE '%' || normalizar_busca(:termo) || '%'
               AND (CAST(:categoria AS TEXT) IS NULL OR p.categoria = :categoria)
            """;

    String CONSULTA_DO_CATALOGO = """
            SELECT p.id, p.nome, p.tipo, p.categoria, p.imagem_url AS imagemUrl,
                   l.nome AS loja, m.url, m.valor_centavos AS valorCentavos,
                   m.valor_original_centavos AS valorOriginalCentavos, m.desconto_pct AS descontoPct,
                   m.expira_em AS expiraEm, m.coletado_em AS coletadoEm
              FROM produto p
              LEFT JOIN vw_melhor_oferta_atual m ON m.produto_id = p.id
              LEFT JOIN loja l ON l.id = m.loja_id
            """ + FILTRO_DO_CATALOGO;

    String CONTAGEM_DO_CATALOGO = "SELECT count(*) FROM produto p" + FILTRO_DO_CATALOGO;

    List<ProdutoEntity> findByChaveItadIn(Collection<String> chavesItad);

    Optional<ProdutoEntity> findByIdAndAtivoTrue(Long id);

    @Query(value = CONSULTA_DO_CATALOGO + " ORDER BY p.nome, p.id",
            countQuery = CONTAGEM_DO_CATALOGO, nativeQuery = true)
    Page<ProdutoResumoProjecao> buscarCatalogoPorNome(@Param("termo") String termo,
                                                      @Param("categoria") String categoria,
                                                      Pageable paginacao);

    @Query(value = CONSULTA_DO_CATALOGO + " ORDER BY m.valor_centavos ASC NULLS LAST, p.id",
            countQuery = CONTAGEM_DO_CATALOGO, nativeQuery = true)
    Page<ProdutoResumoProjecao> buscarCatalogoPorMenorPreco(@Param("termo") String termo,
                                                            @Param("categoria") String categoria,
                                                            Pageable paginacao);
}
