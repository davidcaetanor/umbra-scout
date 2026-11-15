package dev.davidcaetano.umbra_api.usuario.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class CodigosDeTrocaTest {

    private final AtomicLong nanos = new AtomicLong();
    private final CodigosDeTroca codigos = new CodigosDeTroca(nanos::get);

    @Test
    void codigoResgatado_deveDevolverOUsuarioUmaVezSo() {
        String codigo = codigos.emitir(42L);

        assertThat(codigos.resgatar(codigo)).contains(42L);
        assertThat(codigos.resgatar(codigo)).isEmpty();
    }

    @Test
    void codigoVencido_naoDeveSerResgatado() {
        String codigo = codigos.emitir(42L);

        nanos.addAndGet(CodigosDeTroca.VALIDADE.plusMillis(1).toNanos());

        assertThat(codigos.resgatar(codigo)).isEmpty();
    }

    @Test
    void codigoDentroDaValidade_deveSerResgatado() {
        String codigo = codigos.emitir(42L);

        nanos.addAndGet(CodigosDeTroca.VALIDADE.minusSeconds(1).toNanos());

        assertThat(codigos.resgatar(codigo)).contains(42L);
    }

    @Test
    void codigosEmitidos_devemSerDistintosEComEntropiaDe256Bits() {
        Set<String> emitidos = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            emitidos.add(codigos.emitir(1L));
        }

        assertThat(emitidos).hasSize(100).allSatisfy(codigo -> assertThat(codigo).matches("[A-Za-z0-9_-]{43}"));
    }

    @Test
    void codigoNulo_naoDeveSerResgatado() {
        assertThat(codigos.resgatar(null)).isEmpty();
    }
}
