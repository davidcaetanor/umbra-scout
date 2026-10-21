package dev.davidcaetano.umbra_api.config;

import dev.davidcaetano.umbra_api.coleta.ColetaScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "umbra.coleta", name = "habilitada", havingValue = "true")
public class AgendamentoColetaConfig implements SchedulingConfigurer {

    private final ColetaScheduler coletaScheduler;

    public AgendamentoColetaConfig(ColetaScheduler coletaScheduler) {
        this.coletaScheduler = coletaScheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        coletaScheduler.registrarTarefas(registrar);
    }
}
