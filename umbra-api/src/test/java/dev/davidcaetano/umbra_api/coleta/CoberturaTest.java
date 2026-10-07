package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoberturaTest {

    private static final String GID_PEDIDO = "018d0000-0000-7000-8000-000000000001";
    private static final String GID_NAO_PEDIDO = "018d0000-0000-7000-8000-000000000002";

    @Test
    void amostra_naoDeveSerCenso() {
        assertThat(Cobertura.amostra().ehCenso()).isFalse();
    }

    @Test
    void amostra_naoDeveCobrirNenhumaOferta() {
        assertThat(Cobertura.amostra().cobre(CodigoLoja.KABUM, null)).isFalse();
    }

    @Test
    void censoDe_deveSerCensoECarregarALoja() {
        Cobertura cobertura = Cobertura.censoDe(CodigoLoja.KABUM);

        assertThat(cobertura.ehCenso()).isTrue();
        assertThat(cobertura.lojasEmCenso()).containsExactly(CodigoLoja.KABUM);
    }

    @Test
    void censoDe_deveCobrirQualquerProdutoDaLojaComOuSemChave() {
        Cobertura cobertura = Cobertura.censoDe(CodigoLoja.KABUM);

        assertThat(cobertura.cobre(CodigoLoja.KABUM, null)).isTrue();
        assertThat(cobertura.cobre(CodigoLoja.KABUM, GID_PEDIDO)).isTrue();
        assertThat(cobertura.cobre(CodigoLoja.STEAM, null)).isFalse();
    }

    @Test
    void censoDosProdutos_deveCobrirSoAChavePedidaNasLojasPedidas() {
        Cobertura cobertura = Cobertura.censoDosProdutos(Set.of(CodigoLoja.STEAM, CodigoLoja.GOG), Set.of(GID_PEDIDO));

        assertThat(cobertura.ehCenso()).isTrue();
        assertThat(cobertura.cobre(CodigoLoja.STEAM, GID_PEDIDO)).isTrue();
        assertThat(cobertura.cobre(CodigoLoja.GOG, GID_PEDIDO)).isTrue();
        assertThat(cobertura.cobre(CodigoLoja.STEAM, GID_NAO_PEDIDO)).isFalse();
        assertThat(cobertura.cobre(CodigoLoja.STEAM, null)).isFalse();
        assertThat(cobertura.cobre(CodigoLoja.KABUM, GID_PEDIDO)).isFalse();
    }

    @Test
    void censoDosProdutos_deveRecusarConjuntoDeChavesVazio() {
        Set<CodigoLoja> lojas = Set.of(CodigoLoja.STEAM);
        Set<String> semChaves = Set.of();

        assertThatThrownBy(() -> Cobertura.censoDosProdutos(lojas, semChaves))
                .isInstanceOf(IllegalArgumentException.class);
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
        Cobertura cobertura = new Cobertura(original, Set.of());

        original.add(CodigoLoja.STEAM);

        assertThat(cobertura.lojasEmCenso()).containsExactly(CodigoLoja.KABUM);
    }

    @Test
    void deveRejeitarConjuntoDeLojasNulo() {
        Set<String> chaves = Set.of();

        assertThatThrownBy(() -> new Cobertura(null, chaves))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveRejeitarConjuntoDeChavesNulo() {
        Set<CodigoLoja> lojas = Set.of(CodigoLoja.KABUM);

        assertThatThrownBy(() -> new Cobertura(lojas, null))
                .isInstanceOf(NullPointerException.class);
    }
}
