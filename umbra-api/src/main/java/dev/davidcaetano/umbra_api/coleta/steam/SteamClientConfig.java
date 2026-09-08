package dev.davidcaetano.umbra_api.coleta.steam;

import dev.davidcaetano.umbra_api.coleta.steam.properties.SteamProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class SteamClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);


    @Bean
    public RestClient steamRestClient(RestClient.Builder builder, SteamProperties steamProperties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return builder
                .baseUrl(steamProperties.steamApi().baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

}
