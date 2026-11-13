package dev.davidcaetano.umbra_api.usuario.viacep;

import dev.davidcaetano.umbra_api.usuario.viacep.properties.ViaCepProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class ViaCepClientConfig {

    @Bean
    public RestClient viaCepRestClient(RestClient.Builder builder, ViaCepProperties viaCepProperties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(viaCepProperties.timeoutConexao())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(viaCepProperties.timeoutLeitura());

        return builder
                .baseUrl(viaCepProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
