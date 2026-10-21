package dev.davidcaetano.umbra_api.config;

import dev.davidcaetano.umbra_api.coleta.ColetaScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AgendamentoColetaConfigTest {

    private final ColetaScheduler coletaScheduler = mock(ColetaScheduler.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(contexto -> {
                MutablePropertySources fontes = contexto.getEnvironment().getPropertySources();
                fontes.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
                fontes.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            })
            .withBean(ColetaScheduler.class, () -> coletaScheduler)
            .withUserConfiguration(AgendamentoColetaConfig.class);

    @Test
    void deveRegistrarTarefasQuandoColetaHabilitada() {
        contextRunner
                .withPropertyValues("umbra.coleta.habilitada=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(AgendamentoColetaConfig.class);
                    verify(coletaScheduler).registrarTarefas(any());
                });
    }

    @Test
    void naoDeveAgendarNadaQuandoColetaDesabilitada() {
        contextRunner
                .withPropertyValues("umbra.coleta.habilitada=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AgendamentoColetaConfig.class);
                    assertThat(context).doesNotHaveBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
                    verify(coletaScheduler, never()).registrarTarefas(any());
                });
    }

    @Test
    void naoDeveAgendarNadaQuandoFlagAusente() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(AgendamentoColetaConfig.class);
            assertThat(context).doesNotHaveBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
            verify(coletaScheduler, never()).registrarTarefas(any());
        });
    }
}
