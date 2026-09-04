package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.LojaEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LojaRepository extends JpaRepository<LojaEntity, Short> {

    Optional<LojaEntity> findByCodigo(CodigoLoja codigo);
}
