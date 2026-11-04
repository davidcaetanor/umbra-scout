package dev.davidcaetano.umbra_api.catalogo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdenacaoCatalogoTest {

    @Test
    void deCodigo_deveDevolverAConstanteDeCadaCodigo() {
        assertThat(OrdenacaoCatalogo.deCodigo("nome")).isEqualTo(OrdenacaoCatalogo.NOME);
        assertThat(OrdenacaoCatalogo.deCodigo("menorPreco")).isEqualTo(OrdenacaoCatalogo.MENOR_PRECO);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"NOME", "MENOR_PRECO"})
    void deCodigo_deveRecusarOQueNaoEstaNaListaBranca(String codigo) {
        assertThatThrownBy(() -> OrdenacaoCatalogo.deCodigo(codigo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ordenação desconhecida");
    }

    @Test
    void toString_deveDevolverOCodigo() {
        assertThat(OrdenacaoCatalogo.NOME).hasToString("nome");
        assertThat(OrdenacaoCatalogo.MENOR_PRECO).hasToString("menorPreco");
    }
}
