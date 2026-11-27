package dev.davidcaetano.umbra_api.notificacao.repository;

import dev.davidcaetano.umbra_api.notificacao.entity.AvisoDeAlvoEntity;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AvisoDeAlvoRepository extends Repository<AvisoDeAlvoEntity, Long> {

    AvisoDeAlvoEntity save(AvisoDeAlvoEntity aviso);

    List<AvisoDeAlvoEntity> findByMonitoramentoIdOrderByRegistradoEm(Long monitoramentoId);
}
