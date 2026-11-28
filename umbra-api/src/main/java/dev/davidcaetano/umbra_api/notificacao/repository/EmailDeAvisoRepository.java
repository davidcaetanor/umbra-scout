package dev.davidcaetano.umbra_api.notificacao.repository;

import dev.davidcaetano.umbra_api.notificacao.entity.EmailDeAvisoEntity;
import org.springframework.data.repository.Repository;

import java.time.OffsetDateTime;

public interface EmailDeAvisoRepository extends Repository<EmailDeAvisoEntity, Long> {

    EmailDeAvisoEntity save(EmailDeAvisoEntity email);

    long countByUsuarioIdAndEnviadoEmGreaterThanEqual(Long usuarioId, OffsetDateTime desde);
}
