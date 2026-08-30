package dev.davidcaetano.umbra_api.migration;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
public class V1MigrationConstraintsTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    private Long lojaIdSteam() {
        return jdbc.queryForObject("SELECT id FROM loja WHERE codigo = 'STEAM'", Long.class);
    }

    private Long inserirProdutoValido() {
        return jdbc.queryForObject("""
                INSERT INTO produto (loja_id, identificador_loja, nome, tipo, url)
                VALUES (?, 'app-777', 'Black Myth: Wukong', 'JOGO', 'https://store.steampowered.com/app/730')
                RETURNING id
                """, Long.class, lojaIdSteam());
    }

    @Test
    void deveFalharQuandoProdutoDuplicaMesmaLoja() {
        Long lojaId = lojaIdSteam();

        jdbc.update("""
                INSERT INTO produto (loja_id, identificador_loja, nome, tipo, url)
                VALUES (?, 'app-777', 'Black Myth: Wukong', 'JOGO', 'https://store.steampowered.com/app/730')
                """, lojaId);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO produto (loja_id, identificador_loja, nome, tipo, url)
                VALUES (?, 'app-777', 'BL Wukong duplicado', 'JOGO', 'https://store.steampowered.com/app/730')
                """, lojaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_produto_loja");
    }


    @Test
    void deveFalharQuandoTipoDeProdutoInvalido() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO produto (loja_id, identificador_loja, nome, tipo, url)
                VALUES (?, 'app-999', 'Produto Bugado', 'ACESSORIO', 'https://twitter.com')
                """, lojaIdSteam()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_produto_tipo");
    }

    @Test
    void deveFalharQuandoValorOriginalMenorQueAtual() {
        Long produtoId = inserirProdutoValido();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO preco (produto_id, valor_centavos, valor_original_centavos, origem_coleta)
                VALUES (?, 10000, 5000, 'STEAM_API')
                """, produtoId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_preco_original");
    }

    @Test
    void deveDesempatarPrecoAtualPorIdMaisRecenteEmColetasSimultaneas() {
        Long produtoId = inserirProdutoValido();
        Instant mesmoSegundo = Instant.parse("2026-08-30T12:00:00Z");

        Long precoAntigoId = jdbc.queryForObject("""
                INSERT INTO preco (produto_id, valor_centavos, origem_coleta, coletado_em)
                VALUES (?, 9000, 'STEAM_API', ?)
                RETURNING id
                """, Long.class, produtoId, Timestamp.from(mesmoSegundo));

        Long precoMaisRecenteId = jdbc.queryForObject("""
                INSERT INTO preco (produto_id, valor_centavos, origem_coleta, coletado_em)
                VALUES (?, 8000, 'STEAM_API', ?)
                RETURNING id
                """, Long.class, produtoId, Timestamp.from(mesmoSegundo));

        assertThat(precoMaisRecenteId).isGreaterThan(precoAntigoId);

        Long precoIdDaView = jdbc.queryForObject("""
                SELECT preco_id FROM vw_preco_atual WHERE produto_id = ?
                """, Long.class, produtoId);

        assertThat(precoIdDaView).isEqualTo(precoMaisRecenteId);
    }

    @Test
    void deveFalharQuandoDescontoSuperiorA100() {
        Long produtoId = inserirProdutoValido();

        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO preco (produto_id, valor_centavos, desconto_pct, origem_coleta)
            VALUES (?, 10000, 101, 'STEAM_API')
            """, produtoId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_preco_desconto");
    }

    @Test
    void deveFalharQuandoOrigemDesconhecida() {
        Long produtoId = inserirProdutoValido();

        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO preco (produto_id, valor_centavos, origem_coleta)
            VALUES (?, 10000, 'SHURIKEN_STORE')
            """, produtoId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_preco_origem");
    }

    @Test
    void deveFalharQuandoProdutoEstiverEmLojaInexistente() {
        assertThatThrownBy(() -> jdbc.update("""
            INSERT INTO produto (loja_id, identificador_loja, nome, tipo, url)
            VALUES (9999, 'HASAGI_STORE', 'Ultimo Suspiro', 'JOGO', 'https://x.com')
            """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_produto_loja");
    }

    @Test
    void deveFalharQuandoDeletarProdutoComHistoricoDePreco() {
        Long produtoId = inserirProdutoValido();
        jdbc.update("""
            INSERT INTO preco (produto_id, valor_centavos, origem_coleta)
            VALUES (?, 10000, 'STEAM_API')
            """, produtoId);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM produto WHERE id = ?", produtoId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_preco_produto");
    }

}
