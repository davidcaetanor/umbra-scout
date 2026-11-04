package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.properties.BuscaProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TetoDeBuscaExterna {

    private final RateLimiter limitador;

    @Autowired
    public TetoDeBuscaExterna(BuscaProperties buscaProperties) {
        this(buscaProperties.externa().teto(), buscaProperties.externa().janela());
    }

    public TetoDeBuscaExterna(int teto, Duration janela) {
        this.limitador = RateLimiter.of("busca-externa", RateLimiterConfig.custom()
                .limitForPeriod(teto)
                .limitRefreshPeriod(janela)
                .timeoutDuration(Duration.ZERO)
                .build());
    }

    public boolean permite() {
        return limitador.acquirePermission();
    }
}
