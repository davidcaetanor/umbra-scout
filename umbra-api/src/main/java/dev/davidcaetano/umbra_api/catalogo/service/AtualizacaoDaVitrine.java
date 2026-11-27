package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.repository.ProdutoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class AtualizacaoDaVitrine {

    private final ProdutoRepository produtoRepository;
    private final Clock clock;
    private final ReentrantLock umaDeCadaVez = new ReentrantLock();

    public AtualizacaoDaVitrine(ProdutoRepository produtoRepository, Clock clock) {
        this.produtoRepository = produtoRepository;
        this.clock = clock;
    }

    @Transactional
    public void atualizar() {
        umaDeCadaVez.lock();
        try {
            Instant inicio = clock.instant();
            produtoRepository.atualizarVitrine();
            log.info("Vitrine atualizada: duracaoMs={} precosNoHistorico={}",
                    Duration.between(inicio, clock.instant()).toMillis(),
                    produtoRepository.estimarPrecosDoHistorico());
        } finally {
            umaDeCadaVez.unlock();
        }
    }
}
