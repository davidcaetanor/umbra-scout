package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.OfertaEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OfertaRepository extends JpaRepository<OfertaEntity, Long> {

    @EntityGraph(attributePaths = "produto")
    List<OfertaEntity> findByLojaIdAndIdentificadorLojaIn(Short lojaId, Collection<String> identificadores);

    @EntityGraph(attributePaths = "produto")
    List<OfertaEntity> findByLojaCodigo(CodigoLoja codigo);

    @Query("""
            SELECT o
              FROM OfertaEntity o
              JOIN FETCH o.produto p
             WHERE o.loja.codigo = :codigoLoja
               AND o.ativa
               AND p.ativo
               AND p.tipo = :tipoProduto
               AND p.chaveItad IS NULL
             ORDER BY o.id
            """)
    List<OfertaEntity> findAtivasDeProdutoAtivoSemChaveItad(@Param("codigoLoja") CodigoLoja codigoLoja,
                                                          @Param("tipoProduto") TipoProduto tipoProduto);
}
