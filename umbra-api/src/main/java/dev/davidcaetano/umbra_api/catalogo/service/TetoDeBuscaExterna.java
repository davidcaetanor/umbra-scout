package dev.davidcaetano.umbra_api.catalogo.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.davidcaetano.umbra_api.catalogo.properties.BuscaProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class TetoDeBuscaExterna {

    // Limite de memória contra quem varia o IP de origem: acima dele o Caffeine descarta os menos usados.
    private static final int MAXIMO_DE_IPS_EM_MEMORIA = 10_000;

    private final RateLimiter global;
    private final RateLimiterConfig configuracaoPorIp;
    private final Cache<String, RateLimiter> porIp;

    @Autowired
    public TetoDeBuscaExterna(BuscaProperties buscaProperties) {
        this(buscaProperties.externa().tetoGlobal(), buscaProperties.externa().tetoPorIp(),
                buscaProperties.externa().janela());
    }

    public TetoDeBuscaExterna(int tetoGlobal, int tetoPorIp, Duration janela) {
        this.global = RateLimiter.of("busca-externa-global", configuracao(tetoGlobal, janela));
        this.configuracaoPorIp = configuracao(tetoPorIp, janela);
        // Expira após uma janela sem acesso: a essa altura o balde do IP já estaria cheio, então descartar não perde contagem.
        this.porIp = Caffeine.newBuilder()
                .expireAfterAccess(janela)
                .maximumSize(MAXIMO_DE_IPS_EM_MEMORIA)
                .build();
    }

    public Optional<TetoAtingido> consumir(String ip) {
        RateLimiter doIp = porIp.get(ip, chave -> RateLimiter.of("busca-externa-por-ip", configuracaoPorIp));

        // O Resilience4j não devolve permissão: o IP sem saldo é recusado antes de gastar a cota global.
        if (doIp.getMetrics().getAvailablePermissions() <= 0) {
            return Optional.of(TetoAtingido.POR_IP);
        }
        if (!global.acquirePermission()) {
            return Optional.of(TetoAtingido.GLOBAL);
        }
        if (!doIp.acquirePermission()) {
            return Optional.of(TetoAtingido.POR_IP);
        }
        return Optional.empty();
    }

    private static RateLimiterConfig configuracao(int teto, Duration janela) {
        return RateLimiterConfig.custom()
                .limitForPeriod(teto)
                .limitRefreshPeriod(janela)
                .timeoutDuration(Duration.ZERO)
                .build();
    }
}
