package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.properties.QualidadeProperties;
import dev.davidcaetano.umbra_api.catalogo.repository.JogoParaConsultarProjecao;
import dev.davidcaetano.umbra_api.catalogo.repository.QualidadeJogoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtualizacaoDaQualidade {

    private final QualidadeJogoRepository qualidadeJogoRepository;
    private final FonteDeQualidade fonteDeQualidade;
    private final QualidadeProperties properties;
    private final Clock clock;

    public void atualizarLote() {
        OffsetDateTime agora = OffsetDateTime.now(clock);
        List<JogoParaConsultarProjecao> jogos = qualidadeJogoRepository.buscarParaConsultar(
                agora.minus(properties.validade()), properties.lote());
        int consultados = 0;
        int semAvaliacao = 0;

        for (JogoParaConsultarProjecao jogo : jogos) {
            QualidadeDoJogo qualidade;
            try {
                qualidade = fonteDeQualidade.consultar(jogo.getChaveItad()).orElseGet(QualidadeDoJogo::semAvaliacao);
            } catch (RuntimeException falha) {
                log.warn("Atualizacao da qualidade interrompida: consultados={} de {}; o restante fica para a proxima",
                        consultados, jogos.size(), falha);
                return;
            }
            gravar(jogo.getProdutoId(), qualidade, agora);
            consultados++;
            if (qualidade.steamNota() == null && qualidade.criticaNota() == null) {
                semAvaliacao++;
            }
        }

        log.info("Qualidade dos jogos atualizada: consultados={} semAvaliacao={}", consultados, semAvaliacao);
    }

    private void gravar(long produtoId, QualidadeDoJogo qualidade, OffsetDateTime consultadaEm) {
        qualidadeJogoRepository.gravar(produtoId, qualidade.steamNota(), qualidade.steamAvaliacoes(),
                qualidade.criticaNota(), qualidade.criticaAvaliacoes(), qualidade.listaDesejos(),
                qualidade.publicador(), consultadaEm);
    }
}
