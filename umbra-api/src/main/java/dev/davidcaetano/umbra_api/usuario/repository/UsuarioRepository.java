package dev.davidcaetano.umbra_api.usuario.repository;

import dev.davidcaetano.umbra_api.usuario.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    boolean existsByEmail(String email);

    Optional<UsuarioEntity> findByEmail(String email);

    Optional<UsuarioEntity> findByIdAndAtivoTrue(Long id);
}
