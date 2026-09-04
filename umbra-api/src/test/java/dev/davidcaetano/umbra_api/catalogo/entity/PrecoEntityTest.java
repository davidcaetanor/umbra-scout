package dev.davidcaetano.umbra_api.catalogo.entity;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrecoEntityTest {

    private static final OffsetDateTime COLETADO_EM = OffsetDateTime.parse("2026-09-01T09:00:00Z");

    private final ProdutoEntity produto = new ProdutoEntity();

    @Test
    void deveFalharQuandoValorForNegativo() {
        assertThatThrownBy(() -> PrecoEntity.novo(produto, -1L, null, null, true,
                OrigemColeta.STEAM_API, COLETADO_EM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valorCentavos");
    }

    @Test
    void deveFalharQuandoValorOriginalMenorQueAtual() {
        assertThatThrownBy(() -> PrecoEntity.novo(produto, 10000L, 5000L, null, true,
                OrigemColeta.STEAM_API, COLETADO_EM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valorOriginalCentavos");
    }

    @Test
    void deveFalharQuandoDescontoForaDaFaixaPermitida() {
        assertThatThrownBy(() -> PrecoEntity.novo(produto, 10000L, null, (short) 101, true,
                OrigemColeta.STEAM_API, COLETADO_EM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descontoPct");
    }

    @Test
    void deveFalharQuandoOrigemColetaForNula() {
        assertThatThrownBy(() -> PrecoEntity.novo(produto, 10000L, null, null, true,
                null, COLETADO_EM))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("origem");
    }

    @Test
    void deveFalharQuandoProdutoForNulo() {
        assertThatThrownBy(() -> PrecoEntity.novo(null, 10000L, null, null, true,
                OrigemColeta.STEAM_API, COLETADO_EM))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("produto");
    }

    @Test
    void deveCriarPrecoIndisponivelQuandoItemEstiverEsgotado() {
        PrecoEntity preco = PrecoEntity.novo(produto, 10000L, null, null, false,
                OrigemColeta.STEAM_API, COLETADO_EM);

        assertThat(preco.isDisponivel()).isFalse();
    }

    @Test
    void deveAceitarValorZeroQuandoProdutoForGratuito() {
        PrecoEntity preco = PrecoEntity.novo(produto, 0L, null, null, true,
                OrigemColeta.STEAM_API, COLETADO_EM);

        assertThat(preco.getValorCentavos()).isZero();
    }

    @Test
    void deveAceitarValorOriginalIgualAoAtualQuandoNaoHouverDesconto() {
        PrecoEntity preco = PrecoEntity.novo(produto, 10000L, 10000L, (short) 0, true,
                OrigemColeta.STEAM_API, COLETADO_EM);

        assertThat(preco.getValorOriginalCentavos()).isEqualTo(preco.getValorCentavos());
    }
}
