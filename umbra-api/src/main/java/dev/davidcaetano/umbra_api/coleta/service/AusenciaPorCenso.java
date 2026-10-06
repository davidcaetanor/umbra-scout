package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.catalogo.entity.PrecoEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaComUltimoPrecoProjecao;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.PrecoRepository;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.service.RegraDeGravacaoDePreco.UltimoPreco;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Em rodada de censo que viu tudo, a oferta ativa do escopo que não voltou ganha uma linha
 * indisponível com o último valor conhecido (ARCHITECTURE.md §4.11). {@code oferta.ativa} não é tocado.
 */
@Slf4j
@Component
class AusenciaPorCenso {

    private final PrecoRepository precoRepository;
    private final OfertaRepository ofertaRepository;
    private final TransactionTemplate transactionTemplate;
    private final RegraDeGravacaoDePreco regraDeGravacaoDePreco = new RegraDeGravacaoDePreco();

    AusenciaPorCenso(PrecoRepository precoRepository,
                     OfertaRepository ofertaRepository,
                     TransactionTemplate transactionTemplate) {

        this.precoRepository = precoRepository;
        this.ofertaRepository = ofertaRepository;
        this.transactionTemplate = transactionTemplate;
    }

    void concluir(ResultadoColeta resultado, VereditoRodada veredito, OffsetDateTime agora) {

        if (!resultado.cobertura().ehCenso()) {
            return;
        }

        if (!rodadaViuTudoQuantoPretendia(resultado, veredito)) {
            Reconciliacao reconciliacao = resultado.reconciliacao();
            log.info("Censo sem conclusao de ausencia: fonte={} situacao={} semPreco={} declarado={} distintos={}",
                    resultado.fonte(), veredito.situacao(), resultado.totalSemPreco(), reconciliacao.declarado(),
                    reconciliacao.distintos());
            return;
        }

        List<OfertaComUltimoPrecoProjecao> conhecidas = precoRepository.findOfertasAtivasComUltimoPrecoPorCodigoLojaIn(
                resultado.cobertura().lojasEmCenso().stream().map(CodigoLoja::name).toList());

        Set<OfertaVista> vistas = resultado.ofertas().stream()
                .map(oferta -> new OfertaVista(oferta.loja(), oferta.identificadorLoja()))
                .collect(Collectors.toSet());

        List<OfertaComUltimoPrecoProjecao> aMarcar = conhecidas.stream()
                .filter(conhecida -> !vistas.contains(
                        new OfertaVista(CodigoLoja.valueOf(conhecida.getCodigoLoja()), conhecida.getIdentificadorLoja())))
                .filter(ausente -> regraDeGravacaoDePreco.devePersistir(false,
                        new UltimoPreco(ausente.getValorCentavos(), ausente.isDisponivel(), ausente.getColetadoEm()),
                        ausente.getValorCentavos(), false, agora.toInstant()))
                .toList();

        transactionTemplate.executeWithoutResult(status -> aMarcar.forEach(ausente ->
                precoRepository.save(PrecoEntity.novo(ofertaRepository.getReferenceById(ausente.getOfertaId()),
                        ausente.getValorCentavos(), null, null, false, resultado.fonte(), null, agora))));

        log.info("Ausencia por censo concluida: fonte={} ofertasConhecidasNoEscopo={} marcadasIndisponiveis={}",
                resultado.fonte(), conhecidas.size(), aMarcar.size());
    }

    /**
     * Mais estreita que o alarme de propósito: gravar tolera ruído, ausência é deduzida de a rodada ter
     * visto tudo. Item que voltou sem preço foi visto e não lido; marcá-lo seria transformar falha de
     * leitura em fato no histórico.
     */
    static boolean rodadaViuTudoQuantoPretendia(ResultadoColeta resultado, VereditoRodada veredito) {
        Reconciliacao reconciliacao = resultado.reconciliacao();
        Long declarado = reconciliacao.declarado();

        return resultado.cobertura().ehCenso()
                && veredito.situacao() == SituacaoRodada.SAUDAVEL
                && resultado.totalSemPreco() == 0
                && (declarado == null || reconciliacao.distintos() >= declarado);
    }

    private record OfertaVista(CodigoLoja loja, String identificadorLoja) {
    }
}
