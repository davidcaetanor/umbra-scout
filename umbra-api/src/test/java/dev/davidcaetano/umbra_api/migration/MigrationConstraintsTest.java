package dev.davidcaetano.umbra_api.migration;


import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class MigrationConstraintsTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private Long lojaId(String codigo) {
        return jdbc.queryForObject("SELECT id FROM loja WHERE codigo = ?", Long.class, codigo);
    }

    private Long inserirProduto(String chaveItad) {
        return jdbc.queryForObject("""
                INSERT INTO produto (tipo, nome, chave_itad)
                VALUES ('JOGO', 'Black Myth: Wukong', ?)
                RETURNING id
                """, Long.class, chaveItad);
    }

    private Long inserirOferta(Long produtoId, Long lojaId, String identificadorLoja) {
        return jdbc.queryForObject("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url)
                VALUES (?, ?, ?, 'https://store.steampowered.com/app/730')
                RETURNING id
                """, Long.class, produtoId, lojaId, identificadorLoja);
    }

    private void inserirPreco(Long ofertaId, long valorCentavos, boolean disponivel, String origemColeta) {
        jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, disponivel, origem_coleta)
                VALUES (?, ?, ?, ?)
                """, ofertaId, valorCentavos, disponivel, origemColeta);
    }

    @Test
    void deveFalharQuandoProdutoDuplicaChaveItad() {
        inserirProduto("018d0000-gid-777");

        assertThatThrownBy(() -> inserirProduto("018d0000-gid-777"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_produto_chave_itad");
    }

    @Test
    void deveAceitarVariosProdutosComChaveItadNula() {
        Long id1 = inserirProduto(null);
        Long id2 = inserirProduto(null);
        Long id3 = inserirProduto(null);

        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();
        assertThat(id3).isNotNull();
    }

    @Test
    void deveFalharQuandoTipoDeProdutoInvalido() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO produto (tipo, nome)
                VALUES ('ACESSORIO', 'Produto Bugado')
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_produto_tipo");
    }

    @Test
    void deveFalharQuandoNomeDoProdutoForSoEspacos() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO produto (tipo, nome)
                VALUES ('JOGO', '   ')
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_produto_nome");
    }

    @Test
    void deveFalharQuandoOfertaDuplicaMesmaLoja() {
        Long lojaId = lojaId("STEAM");
        Long produtoId = inserirProduto(null);
        inserirOferta(produtoId, lojaId, "app-777");

        assertThatThrownBy(() -> inserirOferta(produtoId, lojaId, "app-777"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_oferta_loja");
    }

    @Test
    void deveFalharQuandoOfertaNaoTiverProduto() {
        Long lojaId = lojaId("STEAM");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url)
                VALUES (9999999, ?, 'app-999', 'https://store.steampowered.com/app/999')
                """, lojaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_oferta_produto");
    }

    @Test
    void deveFalharQuandoOfertaEstiverEmLojaInexistente() {
        Long produtoId = inserirProduto(null);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url)
                VALUES (?, 9999, 'HASAGI_STORE', 'https://x.com')
                """, produtoId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_oferta_loja");
    }

    @Test
    void deveFalharQuandoUrlDaOfertaForRelativa() {
        Long produtoId = inserirProduto(null);
        Long lojaId = lojaId("STEAM");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url)
                VALUES (?, ?, 'app-555', '/app/555')
                """, produtoId, lojaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_oferta_url");
    }

    @Test
    void deveFalharQuandoDeletarProdutoComOferta() {
        Long produtoId = inserirProduto(null);
        inserirOferta(produtoId, lojaId("STEAM"), "app-777");

        assertThatThrownBy(() -> jdbc.update("DELETE FROM produto WHERE id = ?", produtoId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_oferta_produto");
    }

    @Test
    void deveFalharQuandoValorOriginalMenorQueAtual() {
        Long produtoId = inserirProduto(null);
        Long ofertaId = inserirOferta(produtoId, lojaId("STEAM"), "app-777");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, valor_original_centavos, origem_coleta)
                VALUES (?, 10000, 5000, 'STEAM_API')
                """, ofertaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_preco_original");
    }

    @Test
    void deveFalharQuandoDescontoSuperiorA100() {
        Long produtoId = inserirProduto(null);
        Long ofertaId = inserirOferta(produtoId, lojaId("STEAM"), "app-777");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, desconto_pct, origem_coleta)
                VALUES (?, 10000, 101, 'STEAM_API')
                """, ofertaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_preco_desconto");
    }

    @Test
    void deveFalharQuandoOrigemDesconhecida() {
        Long produtoId = inserirProduto(null);
        Long ofertaId = inserirOferta(produtoId, lojaId("STEAM"), "app-777");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, origem_coleta)
                VALUES (?, 10000, 'SHURIKEN_STORE')
                """, ofertaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_preco_origem");
    }

    @Test
    void deveFalharQuandoValorDoPrecoForNegativo() {
        Long produtoId = inserirProduto(null);
        Long ofertaId = inserirOferta(produtoId, lojaId("STEAM"), "app-777");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, origem_coleta)
                VALUES (?, -1, 'STEAM_API')
                """, ofertaId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_preco_valor");
    }

    @Test
    void deveDesempatarPrecoAtualPorIdMaisRecenteEmColetasSimultaneas() {
        Long produtoId = inserirProduto(null);
        Long ofertaId = inserirOferta(produtoId, lojaId("STEAM"), "app-777");
        Instant mesmoSegundo = Instant.parse("2026-08-30T12:00:00Z");

        Long precoAntigoId = jdbc.queryForObject("""
                INSERT INTO preco (oferta_id, valor_centavos, origem_coleta, coletado_em)
                VALUES (?, 9000, 'STEAM_API', ?)
                RETURNING id
                """, Long.class, ofertaId, Timestamp.from(mesmoSegundo));

        Long precoMaisRecenteId = jdbc.queryForObject("""
                INSERT INTO preco (oferta_id, valor_centavos, origem_coleta, coletado_em)
                VALUES (?, 8000, 'STEAM_API', ?)
                RETURNING id
                """, Long.class, ofertaId, Timestamp.from(mesmoSegundo));

        assertThat(precoMaisRecenteId).isGreaterThan(precoAntigoId);

        Long precoIdDaView = jdbc.queryForObject("""
                SELECT preco_id FROM vw_preco_atual WHERE oferta_id = ?
                """, Long.class, ofertaId);

        assertThat(precoIdDaView).isEqualTo(precoMaisRecenteId);
    }

    @Test
    void deveDevolverAOfertaMaisBarataNaViewDeMelhorOferta() {
        Long produtoId = inserirProduto(null);
        Long ofertaSteam = inserirOferta(produtoId, lojaId("STEAM"), "steam-1");
        Long ofertaNuuvem = inserirOferta(produtoId, lojaId("NUUVEM"), "nuuvem-1");
        Long ofertaKabum = inserirOferta(produtoId, lojaId("KABUM"), "kabum-1");

        inserirPreco(ofertaSteam, 12000, true, "STEAM_API");
        inserirPreco(ofertaNuuvem, 8990, true, "NUUVEM_API");
        inserirPreco(ofertaKabum, 15000, true, "KABUM_API");

        Long ofertaIdNaView = jdbc.queryForObject("""
                SELECT oferta_id FROM vw_melhor_oferta_atual WHERE produto_id = ?
                """, Long.class, produtoId);
        Long lojaIdNaView = jdbc.queryForObject("""
                SELECT loja_id FROM vw_melhor_oferta_atual WHERE produto_id = ?
                """, Long.class, produtoId);
        Long valorNaView = jdbc.queryForObject("""
                SELECT valor_centavos FROM vw_melhor_oferta_atual WHERE produto_id = ?
                """, Long.class, produtoId);
        Integer totalLinhas = jdbc.queryForObject("""
                SELECT COUNT(*) FROM vw_melhor_oferta_atual WHERE produto_id = ?
                """, Integer.class, produtoId);

        assertThat(ofertaIdNaView).isEqualTo(ofertaNuuvem);
        assertThat(lojaIdNaView).isEqualTo(lojaId("NUUVEM"));
        assertThat(valorNaView).isEqualTo(8990L);
        assertThat(totalLinhas).isEqualTo(1);
    }

    @Test
    void deveIgnorarOfertaIndisponivelNaViewDeMelhorOferta() {
        Long produtoId = inserirProduto(null);
        Long ofertaMaisBarataIndisponivel = inserirOferta(produtoId, lojaId("STEAM"), "steam-2");
        Long ofertaSegundaMaisBarata = inserirOferta(produtoId, lojaId("NUUVEM"), "nuuvem-2");

        inserirPreco(ofertaMaisBarataIndisponivel, 5000, false, "STEAM_API");
        inserirPreco(ofertaSegundaMaisBarata, 9000, true, "NUUVEM_API");

        Long ofertaIdNaView = jdbc.queryForObject("""
                SELECT oferta_id FROM vw_melhor_oferta_atual WHERE produto_id = ?
                """, Long.class, produtoId);

        assertThat(ofertaIdNaView).isEqualTo(ofertaSegundaMaisBarata);
    }

    @Test
    void deveExistirLinhaDaKabumEmLoja() {
        Integer total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM loja
                WHERE codigo = 'KABUM' AND nome = 'KaBuM!' AND url_base = 'https://www.kabum.com.br' AND ativa
                """, Integer.class);

        assertThat(total).isEqualTo(1);
    }

    @Test
    void deveAceitarOrigemKabumApi() {
        Long produtoId = inserirProduto(null);
        Long ofertaId = inserirOferta(produtoId, lojaId("KABUM"), "kabum-777");

        Long precoId = jdbc.queryForObject("""
                INSERT INTO preco (oferta_id, valor_centavos, origem_coleta)
                VALUES (?, 10000, 'KABUM_API')
                RETURNING id
                """, Long.class, ofertaId);

        assertThat(precoId).isNotNull();
    }

}
