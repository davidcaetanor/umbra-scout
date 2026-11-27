package dev.davidcaetano.umbra_api.catalogo.service;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class AtualizacaoDaVitrineTest extends IntegrationTestBase {

    private static final Instant AGORA = Instant.now();

    @Autowired
    private AtualizacaoDaVitrine atualizacaoDaVitrine;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveGuardarAMelhorOfertaEOMenorPrecoJaVistoDoProduto() {
        long produto = produto("t47 Vitrine");
        long steam = oferta(produto, "STEAM");
        long gog = oferta(produto, "GOG");
        preco(steam, 3000, AGORA.minus(Duration.ofDays(3)));
        preco(steam, 5000, AGORA.minus(Duration.ofHours(1)));
        preco(gog, 4500, AGORA.minus(Duration.ofHours(1)));

        atualizacaoDaVitrine.atualizar();

        assertThat(vitrineDe(produto))
                .containsEntry("oferta_id", gog)
                .containsEntry("valor_centavos", 4500L)
                .containsEntry("minimo_centavos", 3000L);
    }

    @Test
    void precoGravado_soEntraNaVitrineDepoisDaAtualizacao() {
        long produto = produto("t47 Atraso");
        long oferta = oferta(produto, "STEAM");
        preco(oferta, 5000, AGORA.minus(Duration.ofHours(2)));
        atualizacaoDaVitrine.atualizar();

        preco(oferta, 4000, AGORA.minus(Duration.ofHours(1)));
        assertThat(vitrineDe(produto)).containsEntry("valor_centavos", 5000L);

        atualizacaoDaVitrine.atualizar();
        assertThat(vitrineDe(produto))
                .containsEntry("valor_centavos", 4000L)
                .containsEntry("minimo_centavos", 4000L);
    }

    @Test
    void deveRegistrarADuracaoEOTamanhoDoHistorico(CapturedOutput saida) {
        atualizacaoDaVitrine.atualizar();

        assertThat(saida.getOut())
                .containsPattern("Vitrine atualizada: duracaoMs=\\d+ precosNoHistorico=\\d+");
    }

    private Map<String, Object> vitrineDe(long produtoId) {
        return jdbc.queryForMap("""
                SELECT oferta_id, valor_centavos, minimo_centavos FROM mv_vitrine_ofertas WHERE produto_id = ?
                """, produtoId);
    }

    private long produto(String nome) {
        return jdbc.queryForObject("INSERT INTO produto (tipo, nome, ativo) VALUES ('JOGO', ?, true) RETURNING id",
                Long.class, nome);
    }

    private long oferta(long produtoId, String codigoLoja) {
        String identificador = UUID.randomUUID().toString();
        return jdbc.queryForObject("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url, ativa)
                VALUES (?, (SELECT id FROM loja WHERE codigo = ?), ?, ?, true)
                RETURNING id
                """, Long.class, produtoId, codigoLoja, identificador, "https://loja.test/" + identificador);
    }

    private void preco(long ofertaId, long valor, Instant coletadoEm) {
        jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, valor_original_centavos, desconto_pct, disponivel,
                                   origem_coleta, coletado_em)
                VALUES (?, ?, ?, 0, true, 'ITAD_API', ?)
                """, ofertaId, valor, valor, Timestamp.from(coletadoEm));
    }
}
