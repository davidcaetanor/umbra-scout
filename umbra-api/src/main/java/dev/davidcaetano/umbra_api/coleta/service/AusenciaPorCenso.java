package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.catalogo.entity.PrecoEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaComUltimoPrecoProjecao;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.PrecoRepository;
import dev.davidcaetano.umbra_api.coleta.Cobertura;
import dev.davidcaetano.umbra_api.coleta.Reconciliacao;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.properties.AusenciaColetaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
class AusenciaPorCenso {

    private final PrecoRepository precoRepository;
    private final OfertaRepository ofertaRepository;
    private final TransactionTemplate transactionTemplate;
    private final RegraDeGravacaoDePreco regraDeGravacaoDePreco;
    private final AusenciaColetaProperties ausenciaColetaProperties;

    AusenciaPorCenso(PrecoRepository precoRepository,
                     OfertaRepository ofertaRepository,
                     TransactionTemplate transactionTemplate,
                     RegraDeGravacaoDePreco regraDeGravacaoDePreco,
                     AusenciaColetaProperties ausenciaColetaProperties) {

        this.precoRepository = precoRepository;
        this.ofertaRepository = ofertaRepository;
        this.transactionTemplate = transactionTemplate;
        this.regraDeGravacaoDePreco = regraDeGravacaoDePreco;
        this.ausenciaColetaProperties = ausenciaColetaProperties;
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

        Cobertura cobertura = resultado.cobertura();
        List<OfertaComUltimoPrecoProjecao> conhecidas = precoRepository.findOfertasAtivasComUltimoPrecoPorCodigoLojaIn(
                        cobertura.lojasEmCenso().stream().map(CodigoLoja::name).toList())
                .stream()
                .filter(oferta -> cobertura.cobre(CodigoLoja.valueOf(oferta.getCodigoLoja()), oferta.getChaveItad()))
                .toList();

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

        int tetoPercentual = ausenciaColetaProperties.tetoPercentual();
        if (passaDoTeto(aMarcar.size(), conhecidas.size(), tetoPercentual)) {
            log.warn("Ausencia por censo acima do teto, nada marcado: fonte={} seriamMarcadas={} ofertasConhecidasNoEscopo={} percentual={} tetoPercentual={}",
                    resultado.fonte(), aMarcar.size(), conhecidas.size(),
                    String.format(Locale.ROOT, "%.1f", aMarcar.size() * 100.0 / conhecidas.size()), tetoPercentual);
            return;
        }

        transactionTemplate.executeWithoutResult(status -> aMarcar.forEach(ausente ->
                precoRepository.save(PrecoEntity.novo(ofertaRepository.getReferenceById(ausente.getOfertaId()),
                        ausente.getValorCentavos(), null, null, false, resultado.fonte(), null, agora))));

        log.info("Ausencia por censo concluida: fonte={} ofertasConhecidasNoEscopo={} marcadasIndisponiveis={}",
                resultado.fonte(), conhecidas.size(), aMarcar.size());
    }


    // Estritamente maior: uma rodada que marca exatamente o teto ainda grava.
    private static boolean passaDoTeto(int aMarcar, int conhecidas, int tetoPercentual) {
        return (long) aMarcar * 100 > (long) tetoPercentual * conhecidas;
    }

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
