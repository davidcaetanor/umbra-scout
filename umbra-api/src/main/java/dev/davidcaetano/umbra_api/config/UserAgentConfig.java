package dev.davidcaetano.umbra_api.config;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class UserAgentConfig {

    private static final String PRODUTO = "Umbra/1.0";

    @Bean
    public RestClientCustomizer userAgentRestClientCustomizer(UmbraProperties umbraProperties) {
        String userAgent = userAgent(umbraProperties.coleta().contato());
        return builder -> builder.defaultHeader(HttpHeaders.USER_AGENT, userAgent);
    }

    public static String userAgent(String contato) {
        return "%s (+mailto:%s)".formatted(PRODUTO, contato);
    }
}
