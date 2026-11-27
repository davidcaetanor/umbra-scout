package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.ProdutoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<ProdutoEntity, Long> {

    String COLUNAS_DO_RESUMO = """
            SELECT p.id, p.nome, p.tipo, p.categoria, p.imagem_url AS imagemUrl,
                   l.nome AS loja, m.url, m.valor_centavos AS valorCentavos,
                   m.valor_original_centavos AS valorOriginalCentavos, m.desconto_pct AS descontoPct,
                   m.expira_em AS expiraEm, m.coletado_em AS coletadoEm, m.verificado_em AS verificadoEm
            """;

    String MELHOR_OFERTA_DO_PRODUTO = """
              FROM produto p
              LEFT JOIN mv_vitrine_ofertas m ON m.produto_id = p.id
              LEFT JOIN loja l ON l.id = m.loja_id
            """;

    String OFERTA_DA_LOJA = """
              FROM produto p
              JOIN (SELECT DISTINCT ON (o.produto_id)
                           o.produto_id, o.loja_id, o.url, v.valor_centavos, v.valor_original_centavos,
                           v.desconto_pct, v.expira_em, v.coletado_em, o.verificado_em, vm.minimo_centavos
                      FROM oferta o
                      JOIN vw_preco_atual v ON v.oferta_id = o.id
                      JOIN loja lj ON lj.id = o.loja_id
                      LEFT JOIN mv_vitrine_ofertas vm ON vm.produto_id = o.produto_id
                     WHERE o.ativa AND v.disponivel AND lj.codigo = :loja
                       AND NOT (v.expira_em IS NOT NULL AND v.expira_em <= now() AND o.verificado_em < v.expira_em)
                     ORDER BY o.produto_id, v.valor_centavos, o.id) m ON m.produto_id = p.id
              JOIN loja l ON l.id = m.loja_id
            """;

    String FILTRO_DO_CATALOGO = """
             WHERE p.ativo
               AND (CAST(:tipo AS TEXT) IS NULL OR p.tipo = :tipo)
               AND (CAST(:categoria AS TEXT) IS NULL OR p.categoria = :categoria)
               AND (CAST(:termo AS TEXT) IS NULL
                    OR normalizar_busca(p.nome) LIKE '%' || normalizar_busca(CAST(:termo AS TEXT)) || '%')
               AND (CAST(:precoMin AS BIGINT) IS NULL OR m.valor_centavos >= CAST(:precoMin AS BIGINT))
               AND (CAST(:precoMax AS BIGINT) IS NULL OR m.valor_centavos <= CAST(:precoMax AS BIGINT))
               AND (CAST(:descontoMin AS INTEGER) IS NULL OR m.desconto_pct >= CAST(:descontoMin AS INTEGER))
               AND (CAST(:ordenacao AS TEXT) <> 'melhoresOfertas'
                    OR (m.valor_original_centavos > m.valor_centavos AND (m.expira_em IS NULL OR m.expira_em > now())))
            """;

    String ORDEM_DO_CATALOGO = """
             ORDER BY
               CASE WHEN CAST(:ordenacao AS TEXT) = 'melhoresOfertas' THEN
                      row_number() OVER (PARTITION BY p.tipo ORDER BY
                      CASE WHEN p.tipo = 'HARDWARE' THEN
                             CAST(:#{#regua.pesoEconomiaHardware} AS DOUBLE PRECISION)
                               * ln(1 + least(m.valor_original_centavos - m.valor_centavos,
                                              CAST(:#{#regua.tetoEconomiaHardwareCentavos} AS BIGINT)))
                               / CAST(:#{#regua.logaritmoDoTetoHardware()} AS DOUBLE PRECISION)
                           + CAST(:#{#regua.pesoPercentualHardware} AS DOUBLE PRECISION)
                               * COALESCE(m.desconto_pct, 0) / 100.0
                           ELSE
                             CAST(:#{#regua.pesoEconomia} AS DOUBLE PRECISION)
                               * ln(1 + (m.valor_original_centavos - m.valor_centavos))
                               / CAST(:#{#regua.logaritmoDaReferencia} AS DOUBLE PRECISION)
                      END
                    + CASE WHEN p.tipo = 'HARDWARE' THEN CAST(:#{#regua.pesoMinimoHardware()} AS DOUBLE PRECISION)
                           ELSE CAST(:#{#regua.pesoMinimo()} AS DOUBLE PRECISION) END
                        * COALESCE(m.minimo_centavos / NULLIF(CAST(m.valor_centavos AS DOUBLE PRECISION), 0), 1)
                        DESC NULLS LAST, p.id)
               END,
               CASE WHEN CAST(:ordenacao AS TEXT) = 'melhoresOfertas' THEN CASE WHEN p.tipo = 'JOGO' THEN 0 ELSE 1 END END,
               CASE WHEN CAST(:ordenacao AS TEXT) = 'maiorDesconto' THEN m.desconto_pct END DESC NULLS LAST,
               CASE WHEN CAST(:ordenacao AS TEXT) = 'menorPreco' THEN m.valor_centavos END ASC NULLS LAST,
               CASE WHEN CAST(:ordenacao AS TEXT) = 'nome' THEN normalizar_busca(p.nome) END,
               CASE WHEN CAST(:ordenacao AS TEXT) = 'nome' THEN p.nome END,
               p.id
            """;

    @Modifying
    @Query(value = "REFRESH MATERIALIZED VIEW CONCURRENTLY mv_vitrine_ofertas", nativeQuery = true)
    void atualizarVitrine();

    @Query(value = "SELECT CAST(greatest(reltuples, 0) AS BIGINT) FROM pg_class WHERE oid = 'preco'::regclass",
            nativeQuery = true)
    long estimarPrecosDoHistorico();

    List<ProdutoEntity> findByChaveItadIn(Collection<String> chavesItad);

    Optional<ProdutoEntity> findByIdAndAtivoTrue(Long id);

    Optional<ProdutoEntity> findByChaveItad(String chaveItad);

    @Query(value = COLUNAS_DO_RESUMO + MELHOR_OFERTA_DO_PRODUTO + FILTRO_DO_CATALOGO + ORDEM_DO_CATALOGO,
            countQuery = "SELECT count(*)" + MELHOR_OFERTA_DO_PRODUTO + FILTRO_DO_CATALOGO, nativeQuery = true)
    Page<ProdutoResumoProjecao> buscarCatalogo(@Param("termo") String termo,
                                               @Param("tipo") String tipo,
                                               @Param("categoria") String categoria,
                                               @Param("precoMin") Long precoMin,
                                               @Param("precoMax") Long precoMax,
                                               @Param("descontoMin") Integer descontoMin,
                                               @Param("ordenacao") String ordenacao,
                                               @Param("regua") ReguaDeOfertas regua,
                                               Pageable paginacao);

    @Query(value = COLUNAS_DO_RESUMO + OFERTA_DA_LOJA + FILTRO_DO_CATALOGO + ORDEM_DO_CATALOGO,
            countQuery = "SELECT count(*)" + OFERTA_DA_LOJA + FILTRO_DO_CATALOGO, nativeQuery = true)
    Page<ProdutoResumoProjecao> buscarCatalogoDaLoja(@Param("loja") String loja,
                                                     @Param("termo") String termo,
                                                     @Param("tipo") String tipo,
                                                     @Param("categoria") String categoria,
                                                     @Param("precoMin") Long precoMin,
                                                     @Param("precoMax") Long precoMax,
                                                     @Param("descontoMin") Integer descontoMin,
                                                     @Param("ordenacao") String ordenacao,
                                                     @Param("regua") ReguaDeOfertas regua,
                                                     Pageable paginacao);
}
