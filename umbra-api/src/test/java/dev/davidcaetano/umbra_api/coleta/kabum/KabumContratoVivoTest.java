package dev.davidcaetano.umbra_api.coleta.kabum;

import dev.davidcaetano.umbra_api.catalogo.entity.OfertaEntity;
import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.repository.OfertaRepository;
import dev.davidcaetano.umbra_api.catalogo.repository.PrecoRepository;
import dev.davidcaetano.umbra_api.coleta.Cobertura;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.service.ColetaService;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("contrato-vivo")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class KabumContratoVivoTest extends IntegrationTestBase {

    @DynamicPropertySource
    static void apontarParaApiReal(DynamicPropertyRegistry registry) {
        registry.add("kabum.kabum-api.base-url", () -> "https://servicespub.prod.api.aws.grupokabum.com.br");
    }

    @Autowired
    private KabumScraper kabumScraper;

    @Autowired
    private ColetaService coletaService;

    @Autowired
    private OfertaRepository ofertaRepository;

    @Autowired
    private PrecoRepository precoRepository;

    @Test
    void varredura_deveFecharCensoContraApiViva() {
        ResultadoColeta resultado = kabumScraper.coletar();

        System.out.println("[contrato-vivo] reconciliacao=" + resultado.reconciliacao()
                + " ofertas=" + resultado.ofertas().size());

        assertThat(kabumScraper.cobertura()).isEqualTo(Cobertura.CENSO);
        assertThat(resultado.reconciliacao().declarado()).isBetween(800L, 1300L);
        assertThat(resultado.reconciliacao().distintos())
                .isEqualTo(resultado.reconciliacao().declarado().intValue());
        assertThat(resultado.reconciliacao().distintos()).isEqualTo(resultado.reconciliacao().brutos());
        assertThat(resultado.ofertas()).isNotEmpty();
    }

    @Test
    void coletar_devePersistirProdutoOfertaEPrecoNoPostgresAPartirDeRodadaReal() {
        ResultadoColeta resultado = kabumScraper.coletar();

        coletaService.gravar(resultado);

        List<OfertaEntity> ofertasGravadas = ofertaRepository.findByLojaCodigo(CodigoLoja.KABUM);

        System.out.println("[contrato-vivo] coletadas=" + resultado.ofertas().size()
                + " gravadas=" + ofertasGravadas.size());

        assertThat(ofertasGravadas).hasSameSizeAs(resultado.ofertas());
        assertThat(ofertasGravadas).allSatisfy(oferta -> {
            assertThat(precoRepository.findUltimoPrecoPorOfertaIdIn(List.of(oferta.getId()))).hasSize(1);
            assertThat(oferta.getProduto()).isNotNull();
        });
    }
}
