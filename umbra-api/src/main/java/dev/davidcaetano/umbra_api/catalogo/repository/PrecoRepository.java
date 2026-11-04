package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.PrecoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrecoRepository extends JpaRepository<PrecoEntity, Long> {

    @Query(value = """
            SELECT oferta_id AS ofertaId, valor_centavos AS valorCentavos, disponivel, coletado_em AS coletadoEm
              FROM vw_preco_atual
             WHERE oferta_id IN (:ofertaIds)
            """, nativeQuery = true)
    List<UltimoPrecoProjecao> findUltimoPrecoPorOfertaIdIn(@Param("ofertaIds") Collection<Long> ofertaIds);

    @Query(value = """
            SELECT o.id AS ofertaId, l.codigo AS codigoLoja, o.identificador_loja AS identificadorLoja,
                   p.chave_itad AS chaveItad, p.nome AS nomeProduto,
                   v.valor_centavos AS valorCentavos, v.disponivel, v.coletado_em AS coletadoEm
              FROM oferta o
              JOIN loja l ON l.id = o.loja_id
              JOIN produto p ON p.id = o.produto_id
              JOIN vw_preco_atual v ON v.oferta_id = o.id
             WHERE o.ativa
               AND p.ativo
               AND l.codigo IN (:codigosLoja)
            """, nativeQuery = true)
    List<OfertaComUltimoPrecoProjecao> findOfertasAtivasComUltimoPrecoPorCodigoLojaIn(
            @Param("codigosLoja") Collection<String> codigosLoja);

    @Query(value = """
            SELECT p.id, p.oferta_id AS ofertaId, l.nome AS loja, p.valor_centavos AS valorCentavos,
                   p.valor_original_centavos AS valorOriginalCentavos, p.disponivel, p.coletado_em AS coletadoEm
              FROM preco p
              JOIN oferta o ON o.id = p.oferta_id
              JOIN loja l ON l.id = o.loja_id
             WHERE o.produto_id = :produtoId AND o.ativa
             ORDER BY p.oferta_id, p.coletado_em, p.id
            """, nativeQuery = true)
    List<PontoDePrecoProjecao> findPontosDoProduto(@Param("produtoId") Long produtoId);

    @Query(value = """
            SELECT p.id, p.oferta_id AS ofertaId, l.nome AS loja, p.valor_centavos AS valorCentavos,
                   p.valor_original_centavos AS valorOriginalCentavos, p.disponivel, p.coletado_em AS coletadoEm
              FROM preco p
              JOIN oferta o ON o.id = p.oferta_id
              JOIN loja l ON l.id = o.loja_id
             WHERE o.produto_id = :produtoId AND o.ativa AND p.coletado_em >= :inicio
             ORDER BY p.oferta_id, p.coletado_em, p.id
            """, nativeQuery = true)
    List<PontoDePrecoProjecao> findPontosDoProdutoDesde(@Param("produtoId") Long produtoId,
                                                         @Param("inicio") Instant inicio);

    @Query(value = """
            SELECT DISTINCT ON (p.oferta_id)
                   p.id, p.oferta_id AS ofertaId, l.nome AS loja, p.valor_centavos AS valorCentavos,
                   p.valor_original_centavos AS valorOriginalCentavos, p.disponivel, p.coletado_em AS coletadoEm
              FROM preco p
              JOIN oferta o ON o.id = p.oferta_id
              JOIN loja l ON l.id = o.loja_id
             WHERE o.produto_id = :produtoId AND o.ativa AND p.coletado_em < :inicio
             ORDER BY p.oferta_id, p.coletado_em DESC, p.id DESC
            """, nativeQuery = true)
    List<PontoDePrecoProjecao> findUltimoPontoDeCadaOfertaAntesDe(@Param("produtoId") Long produtoId,
                                                                   @Param("inicio") Instant inicio);

    @Query(value = """
            SELECT p.id, p.oferta_id AS ofertaId, l.nome AS loja, p.valor_centavos AS valorCentavos,
                   p.valor_original_centavos AS valorOriginalCentavos, p.disponivel, p.coletado_em AS coletadoEm
              FROM preco p
              JOIN oferta o ON o.id = p.oferta_id
              JOIN loja l ON l.id = o.loja_id
             WHERE o.produto_id = :produtoId AND o.ativa AND p.disponivel
             ORDER BY p.valor_centavos, p.coletado_em, p.id
             LIMIT 1
            """, nativeQuery = true)
    Optional<PontoDePrecoProjecao> findMinimoHistoricoDoProduto(@Param("produtoId") Long produtoId);

    @Query(value = """
            SELECT min(p.coletado_em)
              FROM preco p
              JOIN oferta o ON o.id = p.oferta_id
             WHERE o.produto_id = :produtoId AND o.ativa
            """, nativeQuery = true)
    Optional<Instant> findPrimeiraObservacaoDoProduto(@Param("produtoId") Long produtoId);

    @Query(value = "SELECT valor_centavos FROM vw_melhor_oferta_atual WHERE produto_id = :produtoId",
            nativeQuery = true)
    Optional<Long> findPrecoAtualDoProduto(@Param("produtoId") Long produtoId);
}
