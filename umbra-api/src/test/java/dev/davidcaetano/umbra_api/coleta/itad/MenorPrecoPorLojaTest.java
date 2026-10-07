package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadLojaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadOfertaPrecoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadValorResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MenorPrecoPorLojaTest {

    private static final int SHOP_STEAM = 61;
    private static final int SHOP_NUUVEM = 50;

    private static ItadOfertaPrecoResponse oferta(Integer shopId, Integer preco, String url) {
        return new ItadOfertaPrecoResponse(
                shopId == null ? null : new ItadLojaResponse(shopId, "Loja " + shopId),
                preco == null ? null : new ItadValorResponse(preco, "BRL"),
                new ItadValorResponse(9990, "BRL"),
                0, null, null, url);
    }

    @Test
    void escolher_deveFicarComAMaisBarataQuandoAMaisBarataChegarPrimeiro() {
        ItadOfertaPrecoResponse barata = oferta(SHOP_STEAM, 1000, "https://itad.link/barata");
        ItadOfertaPrecoResponse cara = oferta(SHOP_STEAM, 2000, "https://itad.link/cara");

        assertThat(MenorPrecoPorLoja.escolher(List.of(barata, cara))).containsExactly(barata);
    }

    @Test
    void escolher_deveFicarComAMaisBarataQuandoAMaisBarataChegarDepois() {
        ItadOfertaPrecoResponse cara = oferta(SHOP_STEAM, 2000, "https://itad.link/cara");
        ItadOfertaPrecoResponse barata = oferta(SHOP_STEAM, 1000, "https://itad.link/barata");

        assertThat(MenorPrecoPorLoja.escolher(List.of(cara, barata))).containsExactly(barata);
    }

    @Test
    void escolher_deveFicarComAPrimeiraNoEmpate() {
        ItadOfertaPrecoResponse primeira = oferta(SHOP_STEAM, 1000, "https://itad.link/primeira");
        ItadOfertaPrecoResponse segunda = oferta(SHOP_STEAM, 1000, "https://itad.link/segunda");

        assertThat(MenorPrecoPorLoja.escolher(List.of(primeira, segunda))).containsExactly(primeira);
    }

    @Test
    void escolher_deveManterUmaOfertaPorLojaQuandoAsLojasForemDiferentes() {
        ItadOfertaPrecoResponse steam = oferta(SHOP_STEAM, 2000, "https://itad.link/steam");
        ItadOfertaPrecoResponse nuuvem = oferta(SHOP_NUUVEM, 1000, "https://itad.link/nuuvem");

        assertThat(MenorPrecoPorLoja.escolher(List.of(steam, nuuvem))).containsExactly(steam, nuuvem);
    }

    @Test
    void escolher_deveRepassarOfertaSemShopOuSemPrecoSemAgrupar() {
        ItadOfertaPrecoResponse semShop = oferta(null, 500, "https://itad.link/sem-shop");
        ItadOfertaPrecoResponse semPreco = oferta(SHOP_STEAM, null, "https://itad.link/sem-preco");
        ItadOfertaPrecoResponse steam = oferta(SHOP_STEAM, 2000, "https://itad.link/steam");

        assertThat(MenorPrecoPorLoja.escolher(List.of(semShop, semPreco, steam)))
                .containsExactly(semShop, semPreco, steam);
    }
}
