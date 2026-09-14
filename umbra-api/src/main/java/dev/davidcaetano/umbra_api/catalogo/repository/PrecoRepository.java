package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.PrecoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PrecoRepository extends JpaRepository<PrecoEntity, Long> {

    @Query(value = """
            SELECT oferta_id AS ofertaId, valor_centavos AS valorCentavos, disponivel, coletado_em AS coletadoEm
              FROM vw_preco_atual
             WHERE oferta_id IN (:ofertaIds)
            """, nativeQuery = true)
    List<UltimoPrecoProjecao> findUltimoPrecoPorOfertaIdIn(@Param("ofertaIds") Collection<Long> ofertaIds);

    interface UltimoPrecoProjecao {

        Long getOfertaId();

        long getValorCentavos();

        boolean isDisponivel();

        Instant getColetadoEm();
    }
}
