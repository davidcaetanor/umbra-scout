package dev.davidcaetano.umbra_api.notificacao;

import dev.davidcaetano.umbra_api.catalogo.enums.SituacaoDoPreco;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SelecaoDoLoteTest {

    private final SelecaoDoLote selecao = new SelecaoDoLote(2);

    @Test
    void deveUsarAMelhorLojaAbaixoDoAlvoEContarAsOutras() {
        AlvoEmEnvio alvo = alvo(1, "Hades II", 7000, false, List.of(10L),
                loja("Steam", 5990), loja("GOG", 6500), loja("Nuuvem", 7500));

        LoteDoEmail lote = selecao.selecionar(List.of(alvo));

        assertThat(lote.itens()).containsExactly(new ItemDoEmail(1, "Hades II", 5990, "Steam", 7000, 1, false));
        assertThat(lote.avisosEnviados()).containsExactly(10L);
        assertThat(lote.avisosDescartados()).isEmpty();
        assertThat(lote.restantes()).isZero();
        assertThat(lote.vazio()).isFalse();
    }

    @Test
    void deveAceitarLojaExatamenteNoAlvo() {
        LoteDoEmail lote = selecao.selecionar(List.of(alvo(1, "Celeste", 2000, false, List.of(1L), loja("GOG", 2000))));

        assertThat(lote.itens()).singleElement().extracting(ItemDoEmail::precoCentavos).isEqualTo(2000L);
    }

    @Test
    void deveDescartarAlvoQueVoltouAcimaNaReconferencia() {
        AlvoEmEnvio subiu = alvo(1, "Celeste", 2000, false, List.of(7L, 8L), loja("GOG", 2001));

        LoteDoEmail lote = selecao.selecionar(List.of(subiu));

        assertThat(lote.vazio()).isTrue();
        assertThat(lote.avisosDescartados()).containsExactly(7L, 8L);
        assertThat(lote.avisosEnviados()).isEmpty();
    }

    @Test
    void deveIgnorarPrecoQueNaoValeParaAviso() {
        AlvoEmEnvio alvo = alvo(1, "Celeste", 2000, false, List.of(1L),
                new LojaEmEnvio("Steam", 1000, SituacaoDoPreco.VERIFICACAO_ATRASADA), loja("GOG", 1900));

        LoteDoEmail lote = selecao.selecionar(List.of(alvo));

        assertThat(lote.itens()).singleElement().satisfies(item -> {
            assertThat(item.loja()).isEqualTo("GOG");
            assertThat(item.outrasLojas()).isZero();
        });
    }

    @Test
    void deveOrdenarPelaMaiorQuedaRelativaECortarNoTeto() {
        AlvoEmEnvio dezPorCento = alvo(1, "A", 10000, false, List.of(1L), loja("Steam", 9000));
        AlvoEmEnvio metade = alvo(2, "B", 2000, true, List.of(2L), loja("GOG", 1000));
        AlvoEmEnvio vintePorCento = alvo(3, "C", 50000, false, List.of(3L), loja("Kabum", 40000));

        LoteDoEmail lote = selecao.selecionar(List.of(dezPorCento, metade, vintePorCento));

        assertThat(lote.itens()).extracting(ItemDoEmail::produtoId).containsExactly(2L, 3L);
        assertThat(lote.itens().getFirst().baixouDeNovo()).isTrue();
        assertThat(lote.restantes()).isEqualTo(1);
        assertThat(lote.totalDeAlvos()).isEqualTo(3);
        assertThat(lote.avisosEnviados()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void deveDesempatarPeloNomeQuandoAQuedaEIgual() {
        AlvoEmEnvio zelda = alvo(1, "Zelda", 1000, false, List.of(1L), loja("Steam", 500));
        AlvoEmEnvio celeste = alvo(2, "Celeste", 2000, false, List.of(2L), loja("Steam", 1000));

        LoteDoEmail lote = selecao.selecionar(List.of(zelda, celeste));

        assertThat(lote.itens()).extracting(ItemDoEmail::nome).containsExactly("Celeste", "Zelda");
    }

    @Test
    void naoDeveCortarQuandoCabeNoTeto() {
        SelecaoDoLote vinte = new SelecaoDoLote(20);
        List<AlvoEmEnvio> alvos = LongStream.rangeClosed(1, 20)
                .mapToObj(id -> alvo(id, "Jogo " + id, 1000, false, List.of(id), loja("Steam", 900)))
                .toList();

        LoteDoEmail lote = vinte.selecionar(alvos);

        assertThat(lote.itens()).hasSize(20);
        assertThat(lote.restantes()).isZero();
    }

    @Test
    void deveRecusarTetoDeItensNaoPositivo() {
        assertThatThrownBy(() -> new SelecaoDoLote(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static AlvoEmEnvio alvo(long produtoId, String nome, long alvo, boolean baixouDeNovo, List<Long> avisos,
                                    LojaEmEnvio... lojas) {
        return new AlvoEmEnvio(produtoId, nome, alvo, baixouDeNovo, avisos, List.of(lojas));
    }

    private static LojaEmEnvio loja(String nome, long valor) {
        return new LojaEmEnvio(nome, valor, SituacaoDoPreco.VIGENTE);
    }
}
