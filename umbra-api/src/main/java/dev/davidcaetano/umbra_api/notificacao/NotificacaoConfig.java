package dev.davidcaetano.umbra_api.notificacao;

import dev.davidcaetano.umbra_api.notificacao.properties.NotificacaoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificacaoConfig {

    @Bean
    public AvaliadorDeAlvo avaliadorDeAlvo(NotificacaoProperties propriedades) {
        return new AvaliadorDeAlvo(propriedades.tetoEntreAvisos());
    }
}
