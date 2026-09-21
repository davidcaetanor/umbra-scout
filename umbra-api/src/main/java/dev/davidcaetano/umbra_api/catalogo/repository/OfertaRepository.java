package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.OfertaEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OfertaRepository extends JpaRepository<OfertaEntity, Long> {

    @EntityGraph(attributePaths = "produto")
    List<OfertaEntity> findByLojaIdAndIdentificadorLojaIn(Short lojaId, Collection<String> identificadores);

    @EntityGraph(attributePaths = "produto")
    List<OfertaEntity> findByLojaCodigo(CodigoLoja codigo);
}
