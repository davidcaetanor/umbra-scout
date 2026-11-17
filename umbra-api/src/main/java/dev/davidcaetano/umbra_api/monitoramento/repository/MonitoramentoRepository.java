package dev.davidcaetano.umbra_api.monitoramento.repository;

import dev.davidcaetano.umbra_api.monitoramento.entity.MonitoramentoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MonitoramentoRepository extends Repository<MonitoramentoEntity, Long> {

    String RESUMO = """
            SELECT mo.id, mo.produto_id AS produtoId, p.nome, p.imagem_url AS imagemUrl,
                   mo.preco_alvo_centavos AS precoAlvoCentavos, mo.ativo, mo.criado_em AS criadoEm,
                   COALESCE(m.produto_id IS NULL AND (
                        SELECT max(v.coletado_em)
                          FROM oferta o
                          JOIN vw_preco_atual v ON v.oferta_id = o.id
                         WHERE o.produto_id = p.id
                   ) < now() - make_interval(days => :diasParaDescontinuado), FALSE) AS aparentementeDescontinuado,
                   l.nome AS loja, m.url, m.valor_centavos AS valorCentavos,
                   m.valor_original_centavos AS valorOriginalCentavos, m.desconto_pct AS descontoPct,
                   m.expira_em AS expiraEm, m.coletado_em AS coletadoEm
              FROM monitoramento mo
              JOIN produto p ON p.id = mo.produto_id
              LEFT JOIN vw_melhor_oferta_atual m ON m.produto_id = p.id
              LEFT JOIN loja l ON l.id = m.loja_id
             WHERE mo.usuario_id = :usuarioId
            """;

    MonitoramentoEntity save(MonitoramentoEntity monitoramento);

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

    @Query(value = RESUMO + " AND mo.id = :id", nativeQuery = true)
    Optional<MonitoramentoResumoProjecao> buscarResumoDoUsuario(@Param("id") Long id,
                                                                @Param("usuarioId") Long usuarioId,
                                                                @Param("diasParaDescontinuado") int diasParaDescontinuado);
}
