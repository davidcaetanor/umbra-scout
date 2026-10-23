package dev.davidcaetano.umbra_api.coleta.steam;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.coleta.Cobertura;
import dev.davidcaetano.umbra_api.coleta.ResultadoColeta;
import dev.davidcaetano.umbra_api.coleta.service.ColetaService;
import dev.davidcaetano.umbra_api.coleta.service.SituacaoRodada;
import dev.davidcaetano.umbra_api.coleta.service.VereditoRodada;
import dev.davidcaetano.umbra_api.coleta.steam.dto.response.SteamJogoDescobertoResponse;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("contrato-vivo")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SteamContratoVivoTest extends IntegrationTestBase {

    @DynamicPropertySource
    static void apontarParaApiReal(DynamicPropertyRegistry registry) {
        registry.add("steam.steam-api.base-url", () -> "https://store.steampowered.com/api");
        registry.add("umbra.coleta.contato", () -> Objects.requireNonNullElse(System.getenv("UMBRA_COLETA_CONTATO"), ""));
    }

    @Autowired
    private SteamClient steamClient;

    @Autowired
    private SteamScraper steamScraper;

    @Autowired
    private ColetaService coletaService;

    @Test
    void descoberta_deveTrazerSpecialsComDescontoEExpiracao() {
        List<SteamJogoDescobertoResponse> itens = steamClient.buscarDescoberta().specials().items();

        System.out.println("[contrato-vivo] itens=" + itens.size()
                + " appidsDistintos=" + itens.stream().map(SteamJogoDescobertoResponse::id).distinct().count());

        assertThat(itens).isNotEmpty();
        assertThat(itens).allSatisfy(item -> {
            assertThat(item.discounted()).isTrue();
            assertThat(item.discountExpiration()).isPositive();
        });
    }

    @Test
    void coletar_deveMontarOfertasDaLojaComIdentificadorNumerico() {
        ResultadoColeta resultado = steamScraper.coletar();

        System.out.println("[contrato-vivo] reconciliacao=" + resultado.reconciliacao()
                + " elegiveis=" + resultado.totalElegivel()
                + " semPreco=" + resultado.totalSemPreco());

        assertThat(resultado.cobertura()).isEqualTo(Cobertura.amostra());
        assertThat(resultado.reconciliacao().declarado()).isNull();
        assertThat(resultado.reconciliacao().brutos()).isGreaterThanOrEqualTo(resultado.reconciliacao().distintos());
        assertThat(resultado.ofertas()).allSatisfy(oferta -> {
            assertThat(oferta.loja()).isEqualTo(CodigoLoja.STEAM);
            assertThat(oferta.identificadorLoja()).matches("\\d+");
            assertThat(oferta.url()).isEqualTo("https://store.steampowered.com/app/" + oferta.identificadorLoja() + "/");
            assertThat(oferta.valorCentavos()).isNotNegative();
            if (oferta.valorOriginalCentavos() != null) {
                assertThat(oferta.valorOriginalCentavos()).isGreaterThanOrEqualTo(oferta.valorCentavos());
            }
        });
    }

    @Test
    void coletar_deveGravarNoPostgresSemRejeitarARodada() {
        VereditoRodada veredito = coletaService.gravar(steamScraper.coletar());

        System.out.println("[contrato-vivo] situacao=" + veredito.situacao() + " condicoes=" + veredito.condicoes());

        assertThat(veredito.situacao()).isNotEqualTo(SituacaoRodada.REJEITADA);
    }
}
