package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.OfertaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfertaRepository extends JpaRepository<OfertaEntity, Long> {

    Optional<OfertaEntity> findByLojaIdAndIdentificadorLoja(Short lojaId, String identificadorLoja);
}
