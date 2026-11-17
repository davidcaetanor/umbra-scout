package dev.davidcaetano.umbra_api.usuario.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Component
public class CodigosDeTroca {

    static final Duration VALIDADE = Duration.ofSeconds(60);
    private static final int BYTES_DO_CODIGO = 32;
    private static final int MAXIMO_DE_CODIGOS_PENDENTES = 10_000;

    private final SecureRandom aleatorio = new SecureRandom();
    private final Cache<String, Long> pendentes;

    @Autowired
    public CodigosDeTroca() {
        this(Ticker.systemTicker());
    }

    CodigosDeTroca(Ticker ticker) {
        this.pendentes = Caffeine.newBuilder()
                .expireAfterWrite(VALIDADE)
                .maximumSize(MAXIMO_DE_CODIGOS_PENDENTES)
                .ticker(ticker)
                .build();
    }

    public String emitir(Long usuarioId) {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        byte[] bytes = new byte[BYTES_DO_CODIGO];
        aleatorio.nextBytes(bytes);
        String codigo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pendentes.put(codigo, usuarioId);
        return codigo;
    }

    public Optional<Long> resgatar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        pendentes.cleanUp();
        return Optional.ofNullable(pendentes.asMap().remove(codigo));
    }
}
