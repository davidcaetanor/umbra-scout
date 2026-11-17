package dev.davidcaetano.umbra_api.monitoramento.service;

import dev.davidcaetano.umbra_api.catalogo.service.MaterializacaoDeJogo;
import dev.davidcaetano.umbra_api.catalogo.service.MaterializacaoIndisponivelException;
import dev.davidcaetano.umbra_api.comum.log.ContextoDeLog;
import dev.davidcaetano.umbra_api.monitoramento.InclusaoDeJogoIndisponivelException;
import dev.davidcaetano.umbra_api.monitoramento.properties.MonitoramentoProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class InclusaoDeJogoExterno {

    private final MaterializacaoDeJogo materializacaoDeJogo;
    private final Duration prazo;
    private final Duration tentarDeNovoEm;
    private final ExecutorService executor;

    @Autowired
    public InclusaoDeJogoExterno(MaterializacaoDeJogo materializacaoDeJogo,
                                 MonitoramentoProperties monitoramentoProperties) {
        this(materializacaoDeJogo, monitoramentoProperties.externo().prazo(),
                monitoramentoProperties.externo().tentarDeNovoEm(), Executors.newVirtualThreadPerTaskExecutor());
    }

    InclusaoDeJogoExterno(MaterializacaoDeJogo materializacaoDeJogo, Duration prazo, Duration tentarDeNovoEm,
                          ExecutorService executor) {
        this.materializacaoDeJogo = materializacaoDeJogo;
        this.prazo = prazo;
        this.tentarDeNovoEm = tentarDeNovoEm;
        this.executor = executor;
    }

    public void incluir(String gid) {
        Map<String, String> contextoDoLog = ContextoDeLog.copiar();
        CompletableFuture<Void> inclusao = CompletableFuture.runAsync(
                () -> ContextoDeLog.comContexto(contextoDoLog, () -> materializacaoDeJogo.materializar(gid)), executor);

        try {
            inclusao.get(prazo.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException passouDoPrazo) {
            log.warn("Inclusao de jogo externo passou do prazo de {} ms e segue em segundo plano: gid={}",
                    prazo.toMillis(), gid);
            throw new InclusaoDeJogoIndisponivelException(tentarDeNovoEm, passouDoPrazo);
        } catch (ExecutionException falha) {
            throw traduzir(falha.getCause());
        } catch (InterruptedException interrupcao) {
            Thread.currentThread().interrupt();
            throw new InclusaoDeJogoIndisponivelException(tentarDeNovoEm, interrupcao);
        }
    }

    private RuntimeException traduzir(Throwable causa) {
        if (causa instanceof MaterializacaoIndisponivelException indisponivel) {
            return new InclusaoDeJogoIndisponivelException(tentarDeNovoEm, indisponivel);
        }
        if (causa instanceof RuntimeException erroDeNegocioOuInesperado) {
            return erroDeNegocioOuInesperado;
        }
        return new IllegalStateException("Falha inesperada ao incluir jogo externo", causa);
    }

    @PreDestroy
    void encerrar() {
        executor.shutdownNow();
    }
}
