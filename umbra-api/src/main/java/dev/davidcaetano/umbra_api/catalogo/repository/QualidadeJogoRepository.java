package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.QualidadeJogoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

public interface QualidadeJogoRepository extends JpaRepository<QualidadeJogoEntity, Long> {

    @Query(value = """
            SELECT p.id AS produtoId, p.chave_itad AS chaveItad
              FROM produto p
              LEFT JOIN qualidade_jogo q ON q.produto_id = p.id
              LEFT JOIN mv_vitrine_ofertas m ON m.produto_id = p.id
             WHERE p.ativo AND p.tipo = 'JOGO' AND p.chave_itad IS NOT NULL
               AND (q.produto_id IS NULL OR q.consultada_em < :vencidaAntesDe)
             ORDER BY (m.valor_original_centavos > m.valor_centavos) IS TRUE DESC,
                      q.consultada_em NULLS FIRST,
                      m.valor_original_centavos - m.valor_centavos DESC NULLS LAST,
                      p.id
             LIMIT :lote
            """, nativeQuery = true)
    List<JogoParaConsultarProjecao> buscarParaConsultar(@Param("vencidaAntesDe") OffsetDateTime vencidaAntesDe,
                                                        @Param("lote") int lote);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO qualidade_jogo (produto_id, steam_nota, steam_avaliacoes, critica_nota, critica_avaliacoes,
                                        lista_desejos, publicador, consultada_em)
            VALUES (:produtoId, :steamNota, :steamAvaliacoes, :criticaNota, :criticaAvaliacoes,
                    :listaDesejos, :publicador, :consultadaEm)
            ON CONFLICT (produto_id) DO UPDATE
               SET steam_nota = EXCLUDED.steam_nota,
                   steam_avaliacoes = EXCLUDED.steam_avaliacoes,
                   critica_nota = EXCLUDED.critica_nota,
                   critica_avaliacoes = EXCLUDED.critica_avaliacoes,
                   lista_desejos = EXCLUDED.lista_desejos,
                   publicador = EXCLUDED.publicador,
                   consultada_em = EXCLUDED.consultada_em
            """, nativeQuery = true)
    void gravar(@Param("produtoId") long produtoId,
                @Param("steamNota") Integer steamNota,
                @Param("steamAvaliacoes") Integer steamAvaliacoes,
                @Param("criticaNota") Integer criticaNota,
                @Param("criticaAvaliacoes") Integer criticaAvaliacoes,
                @Param("listaDesejos") Integer listaDesejos,
                @Param("publicador") String publicador,
                @Param("consultadaEm") OffsetDateTime consultadaEm);
}
