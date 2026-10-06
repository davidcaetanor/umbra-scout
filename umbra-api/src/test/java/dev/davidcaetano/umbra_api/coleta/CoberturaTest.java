package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoberturaTest {

    @Test
    void amostra_naoDeveSerCenso() {
        assertThat(Cobertura.amostra().ehCenso()).isFalse();
    }

    @Test
    void censoDe_deveSerCensoECarregarALoja() {
        Cobertura cobertura = Cobertura.censoDe(CodigoLoja.KABUM);

        assertThat(cobertura.ehCenso()).isTrue();
        assertThat(cobertura.lojasEmCenso()).containsExactly(CodigoLoja.KABUM);
    }

    @Test
    void lojasEmCenso_naoDeveAceitarAlteracao() {
        Set<CodigoLoja> lojasEmCenso = Cobertura.censoDe(CodigoLoja.KABUM).lojasEmCenso();

        assertThatThrownBy(() -> lojasEmCenso.add(CodigoLoja.STEAM))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void lojasEmCenso_naoDeveRefletirAlteracaoNoConjuntoOriginal() {
        Set<CodigoLoja> original = new HashSet<>(Set.of(CodigoLoja.KABUM));
        Cobertura cobertura = new Cobertura(original);

        original.add(CodigoLoja.STEAM);

        assertThat(cobertura.lojasEmCenso()).containsExactly(CodigoLoja.KABUM);
    }

    @Test
    void deveRejeitarConjuntoNulo() {
        assertThatThrownBy(() -> new Cobertura(null))
                .isInstanceOf(NullPointerException.class);
    }
}
