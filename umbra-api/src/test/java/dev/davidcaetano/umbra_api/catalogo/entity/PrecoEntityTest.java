package dev.davidcaetano.umbra_api.catalogo.entity;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrecoEntityTest {

    private static final OffsetDateTime COLETADO_EM = OffsetDateTime.parse("2026-09-01T09:00:00Z");

    private final OfertaEntity oferta = new OfertaEntity();

    @Test
    void deveFalharQuandoValorForNegativo() {
        assertThatThrownBy(() -> PrecoEntity.novo(oferta, -1L, null, null, true,
                OrigemColeta.STEAM_API, null, COLETADO_EM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valorCentavos");
    }

    @Test
    void deveFalharQuandoValorOriginalMenorQueAtual() {
        assertThatThrownBy(() -> PrecoEntity.novo(oferta, 10000L, 5000L, null, true,
                OrigemColeta.STEAM_API, null, COLETADO_EM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valorOriginalCentavos");
    }

    @Test
    void deveFalharQuandoDescontoForaDaFaixaPermitida() {
        assertThatThrownBy(() -> PrecoEntity.novo(oferta, 10000L, null, (short) 101, true,
                OrigemColeta.STEAM_API, null, COLETADO_EM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descontoPct");
    }

    @Test
    void deveFalharQuandoOrigemColetaForNula() {
        assertThatThrownBy(() -> PrecoEntity.novo(oferta, 10000L, null, null, true,
                null, null, COLETADO_EM))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("origem");
    }

    @Test
    void deveFalharQuandoOfertaForNula() {
        assertThatThrownBy(() -> PrecoEntity.novo(null, 10000L, null, null, true,
                OrigemColeta.STEAM_API, null, COLETADO_EM))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("oferta");
    }

    @Test
    void deveCriarPrecoIndisponivelQuandoItemEstiverEsgotado() {
        PrecoEntity preco = PrecoEntity.novo(oferta, 10000L, null, null, false,
                OrigemColeta.STEAM_API, null, COLETADO_EM);

        assertThat(preco.isDisponivel()).isFalse();
    }

    @Test
    void deveAceitarValorZeroQuandoProdutoForGratuito() {
        PrecoEntity preco = PrecoEntity.novo(oferta, 0L, null, null, true,
                OrigemColeta.STEAM_API, null, COLETADO_EM);

        assertThat(preco.getValorCentavos()).isZero();
    }

    @Test
    void deveAceitarValorOriginalIgualAoAtualQuandoNaoHouverDesconto() {
        PrecoEntity preco = PrecoEntity.novo(oferta, 10000L, 10000L, (short) 0, true,
                OrigemColeta.STEAM_API, null, COLETADO_EM);

        assertThat(preco.getValorOriginalCentavos()).isEqualTo(preco.getValorCentavos());
    }

    @Test
    void devePreservarExpiraEmQuandoInformado() {
        OffsetDateTime expiraEm = OffsetDateTime.parse("2026-09-10T00:00:00Z");

        PrecoEntity preco = PrecoEntity.novo(oferta, 10000L, null, null, true,
                OrigemColeta.STEAM_API, expiraEm, COLETADO_EM);

        assertThat(preco.getExpiraEm()).isEqualTo(expiraEm);

        PrecoEntity semExpiracao = PrecoEntity.novo(oferta, 10000L, null, null, true,
                OrigemColeta.STEAM_API, null, COLETADO_EM);

        assertThat(semExpiracao.getExpiraEm()).isNull();
    }
}
