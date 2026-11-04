package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.properties.BuscaProperties;
import dev.davidcaetano.umbra_api.coleta.itad.properties.ItadProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ItadClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient itadRestClient(RestClient.Builder builder, ItadProperties itadProperties) {
        return montar(builder, itadProperties, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    @Bean
    public RestClient itadBuscaRestClient(RestClient.Builder builder,
                                          ItadProperties itadProperties,
                                          BuscaProperties buscaProperties) {
        BuscaProperties.Externa externa = buscaProperties.externa();
        return montar(builder, itadProperties, externa.timeoutConexao(), externa.timeoutLeitura());
    }

    private static RestClient montar(RestClient.Builder builder,
                                     ItadProperties itadProperties,
                                     Duration timeoutConexao,
                                     Duration timeoutLeitura) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeoutConexao)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeoutLeitura);

        return builder
                .baseUrl(itadProperties.itadApi().baseUrl())
                .defaultHeader("ITAD-API-Key", itadProperties.itadApi().apiKey())
                .requestFactory(requestFactory)
                .build();
    }
}
