package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.catalogo.enums.CategoriaHardware;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroDoCatalogoTest {

    @Test
    void admiteJogo_semTipoOuComTipoJogo() {
        assertThat(filtro(null, null).admiteJogo()).isTrue();
        assertThat(filtro(TipoProduto.JOGO, null).admiteJogo()).isTrue();
    }

    @Test
    void naoAdmiteJogo_comTipoHardwareOuComCategoria() {
        assertThat(filtro(TipoProduto.HARDWARE, null).admiteJogo()).isFalse();
        assertThat(filtro(null, CategoriaHardware.GPU).admiteJogo()).isFalse();
    }

    private static FiltroDoCatalogo filtro(TipoProduto tipo, CategoriaHardware categoria) {
        return new FiltroDoCatalogo("termo", tipo, categoria, null, null, null, null);
    }
}
