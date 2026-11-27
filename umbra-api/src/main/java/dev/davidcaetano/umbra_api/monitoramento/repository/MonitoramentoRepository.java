package dev.davidcaetano.umbra_api.monitoramento.repository;

import dev.davidcaetano.umbra_api.monitoramento.entity.MonitoramentoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MonitoramentoRepository extends Repository<MonitoramentoEntity, Long> {

    String FORA_DE_VENDA = """
            COALESCE(m.produto_id IS NULL AND (
                 SELECT max(v.coletado_em)
                   FROM oferta o
                   JOIN vw_preco_atual v ON v.oferta_id = o.id
                  WHERE o.produto_id = p.id
            ) < now() - make_interval(days => :diasParaDescontinuado), FALSE)""";

    String RESUMO = """
            SELECT mo.id, mo.produto_id AS produtoId, p.nome, p.imagem_url AS imagemUrl,
                   mo.preco_alvo_centavos AS precoAlvoCentavos, mo.ativo, mo.criado_em AS criadoEm,
                   """ + FORA_DE_VENDA + """
                    AS aparentementeDescontinuado,
                   l.nome AS loja, m.url, m.valor_centavos AS valorCentavos,
                   m.valor_original_centavos AS valorOriginalCentavos, m.desconto_pct AS descontoPct,
                   m.expira_em AS expiraEm, m.coletado_em AS coletadoEm, om.verificado_em AS verificadoEm
              FROM monitoramento mo
              JOIN produto p ON p.id = mo.produto_id
              LEFT JOIN vw_melhor_oferta_atual m ON m.produto_id = p.id
              LEFT JOIN oferta om ON om.id = m.oferta_id
              LEFT JOIN loja l ON l.id = m.loja_id
             WHERE mo.usuario_id = :usuarioId
            """;

    MonitoramentoEntity save(MonitoramentoEntity monitoramento);

    MonitoramentoEntity saveAndFlush(MonitoramentoEntity monitoramento);

    Optional<MonitoramentoEntity> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndProdutoId(Long usuarioId, Long produtoId);

    @Modifying
    @Query("DELETE FROM MonitoramentoEntity m WHERE m.id = :id AND m.usuarioId = :usuarioId")
    int apagarDoUsuario(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    @Query(value = RESUMO + " ORDER BY mo.criado_em DESC, mo.id DESC",
            countQuery = "SELECT count(*) FROM monitoramento WHERE usuario_id = :usuarioId",
            nativeQuery = true)
    Page<MonitoramentoResumoProjecao> listarDoUsuario(@Param("usuarioId") Long usuarioId,
                                                      @Param("diasParaDescontinuado") int diasParaDescontinuado,
                                                      Pageable paginacao);

    @Query(value = RESUMO + " AND mo.produto_id = :produtoId ORDER BY mo.criado_em DESC, mo.id DESC",
            countQuery = "SELECT count(*) FROM monitoramento WHERE usuario_id = :usuarioId AND produto_id = :produtoId",
            nativeQuery = true)
    Page<MonitoramentoResumoProjecao> listarDoUsuarioPorProduto(@Param("usuarioId") Long usuarioId,
                                                                @Param("produtoId") Long produtoId,
                                                                @Param("diasParaDescontinuado") int diasParaDescontinuado,
                                                                Pageable paginacao);

    @Query(value = RESUMO + " AND mo.id = :id", nativeQuery = true)
    Optional<MonitoramentoResumoProjecao> buscarResumoDoUsuario(@Param("id") Long id,
                                                                @Param("usuarioId") Long usuarioId,
                                                                @Param("diasParaDescontinuado") int diasParaDescontinuado);

    @Query(value = """
            SELECT mo.id AS monitoramentoId, mo.preco_alvo_centavos AS precoAlvoCentavos,
                   mo.preco_avisado_centavos AS precoAvisadoCentavos, mo.ultimo_aviso_em AS ultimoAvisoEm,
                   """ + FORA_DE_VENDA + """
                    AS foraDeVenda,
                   oa.id AS ofertaId, va.valor_centavos AS valorCentavos, va.expira_em AS expiraEm,
                   oa.verificado_em AS verificadoEm
              FROM monitoramento mo
              JOIN produto p ON p.id = mo.produto_id
              LEFT JOIN vw_melhor_oferta_atual m ON m.produto_id = p.id
              JOIN oferta oa ON oa.produto_id = p.id AND oa.ativa
              JOIN vw_preco_atual va ON va.oferta_id = oa.id AND va.disponivel
             WHERE mo.ativo
             ORDER BY mo.id, oa.id
            """, nativeQuery = true)
    List<OfertaEmAvaliacaoProjecao> listarOfertasDosAtivos(@Param("diasParaDescontinuado") int diasParaDescontinuado);

    @Modifying
    @Query(value = """
            UPDATE monitoramento
               SET ultimo_aviso_em = :agora, preco_avisado_centavos = :precoCentavos
             WHERE id = :id AND ultimo_aviso_em IS NOT DISTINCT FROM CAST(:ultimoAvisoLido AS TIMESTAMPTZ)
            """, nativeQuery = true)
    int registrarAviso(@Param("id") Long id, @Param("agora") OffsetDateTime agora,
                       @Param("precoCentavos") long precoCentavos,
                       @Param("ultimoAvisoLido") OffsetDateTime ultimoAvisoLido);

    @Modifying
    @Query(value = """
            UPDATE monitoramento SET preco_avisado_centavos = NULL
             WHERE id = :id AND preco_avisado_centavos IS NOT NULL
            """, nativeQuery = true)
    int rearmarAviso(@Param("id") Long id);
}
