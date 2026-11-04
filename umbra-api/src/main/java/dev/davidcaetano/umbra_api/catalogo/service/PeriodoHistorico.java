package dev.davidcaetano.umbra_api.catalogo.service;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

public enum PeriodoHistorico {

    TRINTA_DIAS("30d", Period.ofDays(30)),
    NOVENTA_DIAS("90d", Period.ofDays(90)),
    UM_ANO("1a", Period.ofYears(1)),
    TUDO("tudo", null);

    private final String codigo;
    private final Period duracao;

    PeriodoHistorico(String codigo, Period duracao) {
        this.codigo = codigo;
        this.duracao = duracao;
    }

    public static PeriodoHistorico deCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(periodo -> periodo.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Período desconhecido: " + codigo));
    }

    public Optional<Instant> inicio(Instant agora) {
        return Optional.ofNullable(duracao)
                .map(janela -> agora.atZone(ZoneOffset.UTC).minus(janela).toInstant());
    }

    @Override
    public String toString() {
        return codigo;
    }
}
