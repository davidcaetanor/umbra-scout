package dev.davidcaetano.umbra_api.coleta.itad;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LojaItadTest {

    private static final int SHOP_STEAM = 61;
    private static final int SHOP_NUUVEM = 50;
    private static final int SHOP_DESCONHECIDO = 99;

    @Test
    void identificadorNativo_deveRemoverPrefixoAppDaSteam() {
        assertThat(LojaItad.identificadorNativo(SHOP_STEAM, "app/220")).isEqualTo("220");
    }

    @Test
    void identificadorNativo_deveManterIdentificadorDaSteamJaSemPrefixo() {
        assertThat(LojaItad.identificadorNativo(SHOP_STEAM, "220")).isEqualTo("220");
    }

    @Test
    void identificadorNativo_deveManterIdentificadorDaSteamDeOutroTipo() {
        assertThat(LojaItad.identificadorNativo(SHOP_STEAM, "sub/123")).isEqualTo("sub/123");
    }

    @Test
    void identificadorNativo_deveManterIdentificadorDeLojaSemPrefixo() {
        assertThat(LojaItad.identificadorNativo(SHOP_NUUVEM, "app/220")).isEqualTo("app/220");
    }

    @Test
    void identificadorNativo_deveManterIdentificadorDeShopDesconhecido() {
        assertThat(LojaItad.identificadorNativo(SHOP_DESCONHECIDO, "app/220")).isEqualTo("app/220");
    }

    @Test
    void identificadorItad_deveRecolocarPrefixoAppNaSteam() {
        assertThat(LojaItad.identificadorItad(SHOP_STEAM, "220")).isEqualTo("app/220");
    }

    @Test
    void identificadorItad_naoDeveMexerEmIdentificadorDaSteamQueJaTemTipo() {
        assertThat(LojaItad.identificadorItad(SHOP_STEAM, "sub/123")).isEqualTo("sub/123");
    }

    @Test
    void identificadorItad_naoDeveMexerEmLojaSemPrefixo() {
        assertThat(LojaItad.identificadorItad(SHOP_NUUVEM, "220")).isEqualTo("220");
    }

    @Test
    void identificadorItad_deveDevolverOValorOriginalNaIdaEVolta() {
        assertThat(LojaItad.identificadorItad(SHOP_STEAM, LojaItad.identificadorNativo(SHOP_STEAM, "app/220")))
                .isEqualTo("app/220");
        assertThat(LojaItad.identificadorItad(SHOP_STEAM, LojaItad.identificadorNativo(SHOP_STEAM, "sub/123")))
                .isEqualTo("sub/123");
        assertThat(LojaItad.identificadorItad(SHOP_NUUVEM, LojaItad.identificadorNativo(SHOP_NUUVEM, "jogo-x")))
                .isEqualTo("jogo-x");
    }
}
