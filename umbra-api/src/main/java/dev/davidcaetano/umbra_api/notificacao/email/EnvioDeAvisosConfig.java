package dev.davidcaetano.umbra_api.notificacao.email;

import dev.davidcaetano.umbra_api.notificacao.SelecaoDoLote;
import dev.davidcaetano.umbra_api.notificacao.properties.EnvioDeAvisosProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(EnvioDeAvisosProperties.class)
@ConditionalOnProperty(prefix = "umbra.notificacao", name = "envio-habilitado", havingValue = "true")
public class EnvioDeAvisosConfig {

    @Bean
    public SelecaoDoLote selecaoDoLote(EnvioDeAvisosProperties propriedades) {
        return new SelecaoDoLote(propriedades.itensPorEmail());
    }

    @Bean
    public SpringTemplateEngine motorDoEmail() {
        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.addTemplateResolver(modelos("*.html", TemplateMode.HTML, 1));
        motor.addTemplateResolver(modelos("*.txt", TemplateMode.TEXT, 2));
        motor.setEnableSpringELCompiler(true);
        return motor;
    }

    private static ClassLoaderTemplateResolver modelos(String padrao, TemplateMode modo, int ordem) {
        ClassLoaderTemplateResolver modelos = new ClassLoaderTemplateResolver();
        modelos.setPrefix("templates/email/");
        modelos.setResolvablePatterns(Set.of(padrao));
        modelos.setTemplateMode(modo);
        modelos.setOrder(ordem);
        modelos.setCharacterEncoding(StandardCharsets.UTF_8.name());
        modelos.setCacheable(true);
        return modelos;
    }
}
