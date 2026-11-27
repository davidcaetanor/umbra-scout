package dev.davidcaetano.umbra_api.notificacao.repository;

import dev.davidcaetano.umbra_api.notificacao.entity.AvisoDeAlvoEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface AvisoDeAlvoRepository extends Repository<AvisoDeAlvoEntity, Long> {

    AvisoDeAlvoEntity save(AvisoDeAlvoEntity aviso);

    List<AvisoDeAlvoEntity> findByMonitoramentoIdOrderByRegistradoEm(Long monitoramentoId);

    @Modifying
    @Query(value = """
            UPDATE aviso_de_alvo a
               SET situacao = 'DESCARTADO'
              FROM monitoramento mo
              JOIN usuario u ON u.id = mo.usuario_id
             WHERE a.monitoramento_id = mo.id
               AND a.situacao = 'PENDENTE'
               AND (a.registrado_em < :limite OR NOT mo.ativo OR (u.google_id IS NULL AND u.discord_id IS NULL))
            """, nativeQuery = true)
    int descartarSemDestino(@Param("limite") OffsetDateTime limite);

    @Query(value = """
            SELECT a.id AS avisoId, a.motivo AS motivo, mo.id AS monitoramentoId, mo.usuario_id AS usuarioId,
                   u.email AS email, mo.preco_alvo_centavos AS precoAlvoCentavos, p.id AS produtoId, p.nome AS nome
              FROM aviso_de_alvo a
              JOIN monitoramento mo ON mo.id = a.monitoramento_id
              JOIN usuario u ON u.id = mo.usuario_id
              JOIN produto p ON p.id = mo.produto_id
             WHERE a.situacao = 'PENDENTE'
             ORDER BY mo.usuario_id, mo.id, a.registrado_em, a.id
            """, nativeQuery = true)
    List<AvisoPendenteProjecao> listarPendentes();

    @Query(value = """
            SELECT mo.id AS monitoramentoId, l.nome AS loja, va.valor_centavos AS valorCentavos,
                   va.expira_em AS expiraEm, oa.verificado_em AS verificadoEm
              FROM monitoramento mo
              JOIN oferta oa ON oa.produto_id = mo.produto_id AND oa.ativa
              JOIN loja l ON l.id = oa.loja_id
              JOIN vw_preco_atual va ON va.oferta_id = oa.id AND va.disponivel
             WHERE EXISTS (SELECT 1 FROM aviso_de_alvo a WHERE a.monitoramento_id = mo.id AND a.situacao = 'PENDENTE')
            """, nativeQuery = true)
    List<OfertaAtualProjecao> listarOfertasDosPendentes();

    @Modifying
    @Query(value = """
            UPDATE aviso_de_alvo SET situacao = 'ENVIADO', enviado_em = :agora
             WHERE id IN (:ids) AND situacao = 'PENDENTE'
            """, nativeQuery = true)
    int marcarEnviados(@Param("ids") Collection<Long> ids, @Param("agora") OffsetDateTime agora);

    @Modifying
    @Query(value = "UPDATE aviso_de_alvo SET situacao = 'DESCARTADO' WHERE id IN (:ids) AND situacao = 'PENDENTE'",
            nativeQuery = true)
    int descartar(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query(value = """
            UPDATE aviso_de_alvo
               SET tentativas = tentativas + 1,
                   situacao = CASE WHEN tentativas + 1 >= :limite THEN 'FALHOU' ELSE situacao END
             WHERE id IN (:ids) AND situacao = 'PENDENTE'
            """, nativeQuery = true)
    int registrarFalha(@Param("ids") Collection<Long> ids, @Param("limite") int limite);
}
