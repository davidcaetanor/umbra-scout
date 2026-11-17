package dev.davidcaetano.umbra_api.monitoramento.service;

import dev.davidcaetano.umbra_api.catalogo.JogoExternoRecusadoException;
import dev.davidcaetano.umbra_api.catalogo.service.MaterializacaoDeJogo;
import dev.davidcaetano.umbra_api.catalogo.service.MaterializacaoIndisponivelException;
import dev.davidcaetano.umbra_api.monitoramento.InclusaoDeJogoIndisponivelException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InclusaoDeJogoExternoTest {

    private static final Duration PRAZO = Duration.ofMillis(200);
    private static final Duration TENTAR_DE_NOVO_EM = Duration.ofSeconds(60);
    private static final String GID = "018d937f-590c-728b-ac35-38bcff85f086";

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @AfterEach
    void encerrar() {
        executor.shutdownNow();
    }

    private InclusaoDeJogoExterno com(MaterializacaoDeJogo materializacao) {
        return new InclusaoDeJogoExterno(materializacao, PRAZO, TENTAR_DE_NOVO_EM, executor);
    }

    @Test
    void deveConcluirQuandoAMaterializacaoTerminarNoPrazo() {
        AtomicInteger chamadas = new AtomicInteger();

        assertThatCode(() -> com(gid -> chamadas.incrementAndGet()).incluir(GID)).doesNotThrowAnyException();
        assertThat(chamadas).hasValue(1);
    }

    @Test
    void deveResponderIndisponivelQuandoPassarDoPrazoSemEsperarAMaterializacaoAcabar() {
        CountDownLatch liberar = new CountDownLatch(1);
        long inicio = System.nanoTime();

        assertThatThrownBy(() -> com(gid -> aguardar(liberar)).incluir(GID))
                .isInstanceOf(InclusaoDeJogoIndisponivelException.class)
                .satisfies(erro -> assertThat(((InclusaoDeJogoIndisponivelException) erro).tentarDeNovoEm())
                        .isEqualTo(TENTAR_DE_NOVO_EM));

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(2));
        liberar.countDown();
    }

    @Test
    void deveTraduzirIndisponibilidadeDaFonteEm503() {
        assertThatThrownBy(() -> com(gid -> {
            throw new MaterializacaoIndisponivelException("teto atingido");
        }).incluir(GID))
                .isInstanceOf(InclusaoDeJogoIndisponivelException.class)
                .hasCauseInstanceOf(MaterializacaoIndisponivelException.class);
    }

    @Test
    void devePropagarRecusaDeNegocioSemTraduzir() {
        JogoExternoRecusadoException recusa = JogoExternoRecusadoException.naoEJogo(GID);

        assertThatThrownBy(() -> com(gid -> {
            throw recusa;
        }).incluir(GID)).isSameAs(recusa);
    }

    private static void aguardar(CountDownLatch liberar) {
        try {
            liberar.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException interrupcao) {
            Thread.currentThread().interrupt();
        }
    }
}
