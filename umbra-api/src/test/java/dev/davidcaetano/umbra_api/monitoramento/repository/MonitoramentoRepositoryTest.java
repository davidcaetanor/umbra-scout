package dev.davidcaetano.umbra_api.monitoramento.repository;

import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import dev.davidcaetano.umbra_api.monitoramento.entity.MonitoramentoEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class MonitoramentoRepositoryTest extends IntegrationTestBase {

    private static final int DIAS_PARA_DESCONTINUADO = 180;
    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-11-17T12:00:00Z");

    @Autowired
    private MonitoramentoRepository monitoramentoRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private Long inserirUsuario(String email) {
        return jdbc.queryForObject("""
                INSERT INTO usuario (email, senha_hash, nome_exibicao)
                VALUES (?, 'hash', 'Pessoa')
                RETURNING id
                """, Long.class, email);
    }

    private Long inserirProduto(String nome) {
        return jdbc.queryForObject(
                "INSERT INTO produto (tipo, nome) VALUES ('JOGO', ?) RETURNING id", Long.class, nome);
    }

    private Long inserirOferta(Long produtoId, String codigoLoja, String identificador) {
        return jdbc.queryForObject("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url)
                VALUES (?, (SELECT id FROM loja WHERE codigo = ?), ?, 'https://www.gog.com/jogo')
                RETURNING id
                """, Long.class, produtoId, codigoLoja, identificador);
    }

    private void inserirPreco(Long ofertaId, long valor, boolean disponivel, Instant coletadoEm) {
        jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, disponivel, origem_coleta, coletado_em)
                VALUES (?, ?, ?, 'ITAD_API', ?)
                """, ofertaId, valor, disponivel, Timestamp.from(coletadoEm));
    }

    private MonitoramentoEntity monitorar(Long usuarioId, Long produtoId, long alvo, OffsetDateTime criadoEm) {
        return monitoramentoRepository.save(MonitoramentoEntity.novo(usuarioId, produtoId, alvo, criadoEm));
    }

    private static Instant diasAtras(int dias) {
        return Instant.now().minus(dias, ChronoUnit.DAYS);
    }

    private MonitoramentoResumoProjecao resumo(MonitoramentoEntity monitoramento) {
        return monitoramentoRepository.buscarResumoDoUsuario(monitoramento.getId(), monitoramento.getUsuarioId(),
                DIAS_PARA_DESCONTINUADO).orElseThrow();
    }

    @Test
    void deveListarSoOsMonitoramentosDoUsuarioDoMaisRecenteParaOMaisAntigo() {
        Long dono = inserirUsuario("dono@umbra.dev");
        Long outro = inserirUsuario("outro@umbra.dev");
        MonitoramentoEntity antigo = monitorar(dono, inserirProduto("Hades"), 5000, AGORA.minusDays(2));
        MonitoramentoEntity recente = monitorar(dono, inserirProduto("Celeste"), 3000, AGORA);
        monitorar(outro, inserirProduto("Outer Wilds"), 4000, AGORA);

        Page<MonitoramentoResumoProjecao> pagina =
                monitoramentoRepository.listarDoUsuario(dono, DIAS_PARA_DESCONTINUADO, PageRequest.of(0, 20));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(MonitoramentoResumoProjecao::getId)
                .containsExactly(recente.getId(), antigo.getId());
    }

    @Test
    void deveDesempatarPorIdQuandoACriacaoForNoMesmoInstante() {
        Long dono = inserirUsuario("empate@umbra.dev");
        MonitoramentoEntity primeiro = monitorar(dono, inserirProduto("Hades"), 5000, AGORA);
        MonitoramentoEntity segundo = monitorar(dono, inserirProduto("Celeste"), 5000, AGORA);

        Page<MonitoramentoResumoProjecao> pagina =
                monitoramentoRepository.listarDoUsuario(dono, DIAS_PARA_DESCONTINUADO, PageRequest.of(0, 1));

        assertThat(pagina.getTotalPages()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(MonitoramentoResumoProjecao::getId)
                .containsExactly(segundo.getId());
        assertThat(monitoramentoRepository.listarDoUsuario(dono, DIAS_PARA_DESCONTINUADO, PageRequest.of(1, 1))
                .getContent()).extracting(MonitoramentoResumoProjecao::getId).containsExactly(primeiro.getId());
    }

    @Test
    void deveTrazerAMelhorOfertaAtualEntreAsLojas() {
        Long dono = inserirUsuario("melhor@umbra.dev");
        Long produtoId = inserirProduto("Hollow Knight");
        inserirPreco(inserirOferta(produtoId, "GOG", "hk-gog"), 4500, true, diasAtras(1));
        inserirPreco(inserirOferta(produtoId, "GREEN_MAN_GAMING", "hk-gmg"), 3900, true, diasAtras(1));
        MonitoramentoEntity monitoramento = monitorar(dono, produtoId, 3000, AGORA);

        MonitoramentoResumoProjecao resumo = resumo(monitoramento);

        assertThat(resumo.getValorCentavos()).isEqualTo(3900);
        assertThat(resumo.getLoja()).isEqualTo("Green Man Gaming");
        assertThat(resumo.getPrecoAlvoCentavos()).isEqualTo(3000);
        assertThat(resumo.getNome()).isEqualTo("Hollow Knight");
        assertThat(resumo.isAparentementeDescontinuado()).isFalse();
    }

    @Test
    void naoDeveSinalizarDescontinuadoQuandoOJogoSegueAVendaComPrecoEstavelHaMaisDe180Dias() {
        Long dono = inserirUsuario("estavel@umbra.dev");
        Long produtoId = inserirProduto("Stardew Valley");
        inserirPreco(inserirOferta(produtoId, "GOG", "sv-gog"), 2499, true, diasAtras(400));

        MonitoramentoResumoProjecao resumo = resumo(monitorar(dono, produtoId, 1500, AGORA));

        assertThat(resumo.isAparentementeDescontinuado()).isFalse();
        assertThat(resumo.getValorCentavos()).isEqualTo(2499);
    }

    @Test
    void deveSinalizarDescontinuadoQuandoTodasAsOfertasSairamDeVendaHaMaisDe180Dias() {
        Long dono = inserirUsuario("saiu@umbra.dev");
        Long produtoId = inserirProduto("Jogo Retirado");
        Long gog = inserirOferta(produtoId, "GOG", "jr-gog");
        inserirPreco(gog, 2000, true, diasAtras(400));
        inserirPreco(gog, 2000, false, diasAtras(200));
        Long gmg = inserirOferta(produtoId, "GREEN_MAN_GAMING", "jr-gmg");
        inserirPreco(gmg, 2100, false, diasAtras(190));

        MonitoramentoResumoProjecao resumo = resumo(monitorar(dono, produtoId, 1500, AGORA));

        assertThat(resumo.isAparentementeDescontinuado()).isTrue();
        assertThat(resumo.getValorCentavos()).isNull();
    }

    @Test
    void naoDeveSinalizarDescontinuadoQuandoAUltimaSaidaDeVendaForRecente() {
        Long dono = inserirUsuario("recente@umbra.dev");
        Long produtoId = inserirProduto("Fora Ha Pouco");
        Long gog = inserirOferta(produtoId, "GOG", "fp-gog");
        inserirPreco(gog, 2000, false, diasAtras(400));
        Long gmg = inserirOferta(produtoId, "GREEN_MAN_GAMING", "fp-gmg");
        inserirPreco(gmg, 2100, false, diasAtras(10));

        assertThat(resumo(monitorar(dono, produtoId, 1500, AGORA)).isAparentementeDescontinuado()).isFalse();
    }

    @Test
    void naoDeveSinalizarDescontinuadoQuandoUmaOfertaAindaEstiverAVenda() {
        Long dono = inserirUsuario("parcial@umbra.dev");
        Long produtoId = inserirProduto("Parcial");
        inserirPreco(inserirOferta(produtoId, "GOG", "pa-gog"), 2000, false, diasAtras(300));
        inserirPreco(inserirOferta(produtoId, "GREEN_MAN_GAMING", "pa-gmg"), 2100, true, diasAtras(300));

        assertThat(resumo(monitorar(dono, produtoId, 1500, AGORA)).isAparentementeDescontinuado()).isFalse();
    }

    @Test
    void naoDeveSinalizarDescontinuadoNemQuebrarQuandoOProdutoNaoTiverOferta() {
        Long dono = inserirUsuario("semoferta@umbra.dev");

        MonitoramentoResumoProjecao resumo = resumo(monitorar(dono, inserirProduto("Sem Oferta"), 1500, AGORA));

        assertThat(resumo.isAparentementeDescontinuado()).isFalse();
        assertThat(resumo.getValorCentavos()).isNull();
        assertThat(resumo.getLoja()).isNull();
    }

    @Test
    void naoDeveEncontrarMonitoramentoDeOutroUsuario() {
        Long dono = inserirUsuario("a@umbra.dev");
        Long intruso = inserirUsuario("b@umbra.dev");
        MonitoramentoEntity monitoramento = monitorar(dono, inserirProduto("Hades"), 5000, AGORA);

        assertThat(monitoramentoRepository.findByIdAndUsuarioId(monitoramento.getId(), intruso)).isEmpty();
        assertThat(monitoramentoRepository.buscarResumoDoUsuario(monitoramento.getId(), intruso,
                DIAS_PARA_DESCONTINUADO)).isEmpty();
        assertThat(monitoramentoRepository.apagarDoUsuario(monitoramento.getId(), intruso)).isZero();
        assertThat(monitoramentoRepository.findByIdAndUsuarioId(monitoramento.getId(), dono)).isPresent();
    }

    @Test
    void deveApagarSoQuandoOMonitoramentoForDoUsuario() {
        Long dono = inserirUsuario("apaga@umbra.dev");
        MonitoramentoEntity monitoramento = monitorar(dono, inserirProduto("Hades"), 5000, AGORA);

        assertThat(monitoramentoRepository.apagarDoUsuario(monitoramento.getId(), dono)).isEqualTo(1);
        assertThat(monitoramentoRepository.findByIdAndUsuarioId(monitoramento.getId(), dono)).isEmpty();
    }

    @Test
    void deveDizerSeOUsuarioJaMonitoraOProduto() {
        Long dono = inserirUsuario("existe@umbra.dev");
        Long produtoId = inserirProduto("Hades");
        monitorar(dono, produtoId, 5000, AGORA);

        assertThat(monitoramentoRepository.existsByUsuarioIdAndProdutoId(dono, produtoId)).isTrue();
        assertThat(monitoramentoRepository.existsByUsuarioIdAndProdutoId(dono, inserirProduto("Celeste"))).isFalse();
    }
}
