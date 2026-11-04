package dev.davidcaetano.umbra_api.catalogo.service;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.davidcaetano.umbra_api.catalogo.properties.BuscaProperties;
import dev.davidcaetano.umbra_api.comum.log.ContextoDeLog;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;


@Service
@Slf4j
public class BuscaExternaService {

    private static final Duration MARGEM_DO_DONO = Duration.ofSeconds(1);
    private static final int MAXIMO_DE_TERMOS_EM_CACHE = 500;
    private static final Pattern MARCAS_DE_ACENTO = Pattern.compile("\\p{M}+");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private final BuscaExterna buscaExterna;
    private final TetoDeBuscaExterna teto;
    private final Duration prazo;
    private final AsyncCache<String, List<JogoExterno>> cache;
    private final ExecutorService executor;

    @Autowired
    public BuscaExternaService(BuscaExterna buscaExterna, TetoDeBuscaExterna teto, BuscaProperties buscaProperties) {
        this(buscaExterna, teto, buscaProperties.externa().cacheTtl(), buscaProperties.externa().prazo(),
                Executors.newVirtualThreadPerTaskExecutor());
    }

    public BuscaExternaService(BuscaExterna buscaExterna, TetoDeBuscaExterna teto, Duration cacheTtl, Duration prazo,
                               ExecutorService executor) {
        this.buscaExterna = buscaExterna;
        this.teto = teto;
        this.prazo = prazo;
        this.executor = executor;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(cacheTtl)
                .maximumSize(MAXIMO_DE_TERMOS_EM_CACHE)
                .buildAsync();
    }

    public List<JogoExterno> buscar(String termo) {
        String chave = normalizar(termo);
        Map<String, String> contextoDoLog = ContextoDeLog.copiar();
        CompletableFuture<List<JogoExterno>> novo = new CompletableFuture<>();
        CompletableFuture<List<JogoExterno>> doCache = entrarNoCache(chave, novo);

        if (doCache == novo) {
            novo.orTimeout(prazo.plus(MARGEM_DO_DONO).toMillis(), TimeUnit.MILLISECONDS)
                    .whenComplete((resultado, erro) ->
                            ContextoDeLog.comContexto(contextoDoLog, () -> avisarSePassouDoPrazo(erro, termo)));
            iniciar(termo, novo, contextoDoLog);
        }
        return aguardar(doCache, termo);
    }

    @PreDestroy
    void encerrar() {
        executor.shutdownNow();
    }


    private CompletableFuture<List<JogoExterno>> entrarNoCache(String chave, CompletableFuture<List<JogoExterno>> novo) {
        while (true) {
            CompletableFuture<List<JogoExterno>> existente = cache.asMap().putIfAbsent(chave, novo);
            if (existente == null) {
                return novo;
            }
            if (!existente.isCompletedExceptionally()) {
                return existente;
            }
            cache.asMap().remove(chave, existente);
        }
    }

    private void iniciar(String termo, CompletableFuture<List<JogoExterno>> futuro,
                         Map<String, String> contextoDoLog) {
        if (!teto.permite()) {
            futuro.completeExceptionally(new BuscaExternaIndisponivelException("teto de buscas externas esgotado"));
            log.info("Teto de buscas externas esgotado na janela, só o catálogo local: termo={}", termo);
            return;
        }

        try {
            executor.execute(() -> ContextoDeLog.comContexto(contextoDoLog, () -> chamarAPorta(termo, futuro)));
        } catch (RuntimeException ex) {
            futuro.completeExceptionally(ex);
            log.error("Erro inesperado na busca externa, só o catálogo local: termo={}", termo, ex);
        }
    }

    private void chamarAPorta(String termo, CompletableFuture<List<JogoExterno>> futuro) {
        try {
            long inicio = System.nanoTime();
            List<JogoExterno> resultado = buscaExterna.buscar(termo);
            log.info("Busca externa no ITAD: termo={} resultados={} duracaoMs={}", termo, resultado.size(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio));

            futuro.complete(resultado);
        } catch (BuscaExternaIndisponivelException ex) {
            futuro.completeExceptionally(ex);
            log.warn("Fonte da busca externa indisponível, só o catálogo local: termo={} causa={}", termo,
                    ex.getMessage());
        } catch (RuntimeException ex) {
            futuro.completeExceptionally(ex);
            log.error("Erro inesperado na busca externa, só o catálogo local: termo={}", termo, ex);
        }
    }

    private static void avisarSePassouDoPrazo(Throwable erro, String termo) {
        Throwable causa = erro instanceof CompletionException ? erro.getCause() : erro;
        if (causa instanceof TimeoutException) {
            log.warn("Busca externa passou do prazo, só o catálogo local: termo={}", termo);
        }
    }

    private List<JogoExterno> aguardar(CompletableFuture<List<JogoExterno>> futuro, String termo) {
        try {
            return futuro.get(prazo.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException | ExecutionException ex) {
            log.debug("Busca externa compartilhada sem resultado no prazo: termo={} causa={}", termo, ex.toString());
            return List.of();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.debug("Espera da busca externa interrompida: termo={}", termo);
            return List.of();
        }
    }

    private static String normalizar(String termo) {
        String semAcento = MARCAS_DE_ACENTO.matcher(Normalizer.normalize(termo, Normalizer.Form.NFD)).replaceAll("");
        return ESPACOS.matcher(semAcento.toLowerCase(Locale.ROOT).strip()).replaceAll(" ");
    }
}
