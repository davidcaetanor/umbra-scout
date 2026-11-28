package dev.davidcaetano.umbra_api.notificacao.service;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "umbra.notificacao.envio-habilitado=true",
        "umbra.notificacao.envio.agenda=-",
        "umbra.notificacao.envio.remetente=avisos@umbra.test",
        "umbra.notificacao.envio.url-do-front=https://umbra.test",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false"
})
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class DespachoDeAvisosTest extends IntegrationTestBase {

    @RegisterExtension
    static final GreenMailExtension SMTP = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private DespachoDeAvisos despacho;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> usuarios = new ArrayList<>();
    private final List<Long> produtos = new ArrayList<>();

    @AfterEach
    void limpar() {
        usuarios.forEach(id -> jdbc.update("DELETE FROM usuario WHERE id = ?", id));
        produtos.forEach(id -> {
            jdbc.update("DELETE FROM preco WHERE oferta_id IN (SELECT id FROM oferta WHERE produto_id = ?)", id);
            jdbc.update("DELETE FROM oferta WHERE produto_id = ?", id);
            jdbc.update("DELETE FROM produto WHERE id = ?", id);
        });
    }

    @Test
    void deveEnviarUmEmailPorPessoaComTodosOsAlvosEOrdenadoPelaMaiorQueda(CapturedOutput saida) throws Exception {
        long usuario = usuarioGoogle();
        long celeste = aviso(monitoramento(usuario, produtoComPreco("Celeste", "STEAM", 1000), 2000), "ENTRADA", agora());
        long hades = aviso(monitoramento(usuario, produtoComPreco("Hades", "GOG", 9000), 10000), "QUEDA", agora());

        despacho.despachar();

        MimeMessage email = unicoEmailPara(usuario);
        assertThat(email.getSubject()).isEqualTo("2 alvos atingidos");
        assertThat(email.getFrom()[0].toString()).contains("Umbra").contains("avisos@umbra.test");
        String corpo = partes(email);
        assertThat(corpo).contains("https://umbra.test/monitoramentos").contains("Baixou de novo");
        assertThat(corpo.indexOf("Celeste")).isLessThan(corpo.indexOf("Hades"));
        assertThat(situacao(celeste)).isEqualTo("ENVIADO");
        assertThat(situacao(hades)).isEqualTo("ENVIADO");
        assertThat(jdbc.queryForObject("SELECT itens FROM email_de_aviso WHERE usuario_id = ?", Integer.class, usuario))
                .isEqualTo(2);
        assertThat(saida.getOut()).contains("Email de aviso enviado: usuarioId=" + usuario + " itens=2 restantes=0");
        assertThat(saida.getOut().lines().filter(linha -> linha.contains("\"logger_name\":\"dev.davidcaetano")))
                .noneMatch(linha -> linha.contains(email(usuario)));
    }

    @Test
    void naoDeveEnviarParaContaPorSenhaSemEmailConfirmado() {
        long usuario = usuarioPorSenha();
        long aviso = aviso(monitoramento(usuario, produtoComPreco("Celeste", "STEAM", 1000), 2000), "ENTRADA", agora());

        despacho.despachar();

        assertThat(emailsPara(usuario)).isEmpty();
        assertThat(situacao(aviso)).isEqualTo("DESCARTADO");
    }

    @Test
    void deveDescartarQuandoOPrecoVoltouAcimaDoAlvoAntesDoEnvio() {
        long usuario = usuarioGoogle();
        long aviso = aviso(monitoramento(usuario, produtoComPreco("Celeste", "STEAM", 2500), 2000), "ENTRADA", agora());

        despacho.despachar();

        assertThat(emailsPara(usuario)).isEmpty();
        assertThat(situacao(aviso)).isEqualTo("DESCARTADO");
    }

    @Test
    void deveDescartarPendenteMaisVelhoQueAValidade() {
        long usuario = usuarioGoogle();
        long aviso = aviso(monitoramento(usuario, produtoComPreco("Celeste", "STEAM", 1000), 2000), "ENTRADA",
                agora().minus(Duration.ofHours(25)));

        despacho.despachar();

        assertThat(emailsPara(usuario)).isEmpty();
        assertThat(situacao(aviso)).isEqualTo("DESCARTADO");
    }

    @Test
    void deveManterPendenteQuandoOTetoDiarioFoiAtingido() {
        long usuario = usuarioGoogle();
        long aviso = aviso(monitoramento(usuario, produtoComPreco("Celeste", "STEAM", 1000), 2000), "ENTRADA", agora());
        for (int i = 0; i < 3; i++) {
            jdbc.update("INSERT INTO email_de_aviso (usuario_id, enviado_em, itens) VALUES (?, now(), 1)", usuario);
        }

        despacho.despachar();

        assertThat(emailsPara(usuario)).isEmpty();
        assertThat(situacao(aviso)).isEqualTo("PENDENTE");
    }

    @Test
    void deveContarAFalhaEDesistirNaTerceiraTentativa(CapturedOutput saida) {
        long usuario = usuarioGoogle();
        long aviso = aviso(monitoramento(usuario, produtoComPreco("Celeste", "STEAM", 1000), 2000), "ENTRADA", agora());
        SMTP.stop();

        despacho.despachar();
        assertThat(situacao(aviso)).isEqualTo("PENDENTE");
        assertThat(tentativas(aviso)).isEqualTo(1);

        jdbc.update("UPDATE aviso_de_alvo SET tentativas = 2 WHERE id = ?", aviso);
        despacho.despachar();

        assertThat(situacao(aviso)).isEqualTo("FALHOU");
        assertThat(saida.getOut()).contains("Email de aviso falhou: usuarioId=" + usuario);
    }

    @Test
    void deveEscaparONomeDoProdutoNoHtml() throws Exception {
        long usuario = usuarioGoogle();
        aviso(monitoramento(usuario, produtoComPreco("<script>x</script>", "STEAM", 1000), 2000), "ENTRADA", agora());

        despacho.despachar();

        String corpo = html(unicoEmailPara(usuario));
        assertThat(corpo).doesNotContain("<script>x</script>").contains("&lt;script&gt;");
    }

    @Test
    void deveAbreviarNomeMuitoLongo() throws Exception {
        long usuario = usuarioGoogle();
        String longo = "Placa de Video RTX 5070 " + "X".repeat(200);
        aviso(monitoramento(usuario, produtoComPreco(longo, "STEAM", 1000), 2000), "ENTRADA", agora());

        despacho.despachar();

        String corpo = html(unicoEmailPara(usuario));
        assertThat(corpo).contains("Placa de Video RTX 5070 XXX").contains("X...").doesNotContain("X".repeat(90));
    }

    private MimeMessage unicoEmailPara(long usuario) {
        List<MimeMessage> emails = emailsPara(usuario);
        assertThat(emails).hasSize(1);
        return emails.getFirst();
    }

    private List<MimeMessage> emailsPara(long usuario) {
        String destino = email(usuario);
        return Arrays.stream(SMTP.getReceivedMessages())
                .filter(email -> GreenMailUtil.getAddressList(recipients(email)).contains(destino))
                .toList();
    }

    private static String html(MimeMessage email) throws Exception {
        return parteHtml(email.getContent()).orElseThrow(() -> new AssertionError("email sem parte html"));
    }

    private static Optional<String> parteHtml(Object conteudo) throws Exception {
        if (conteudo instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart parte = multipart.getBodyPart(i);
                if (parte.isMimeType("text/html")) {
                    return Optional.of((String) parte.getContent());
                }
                Optional<String> interna = parteHtml(parte.getContent());
                if (interna.isPresent()) {
                    return interna;
                }
            }
        }
        return Optional.empty();
    }

    private static String partes(MimeMessage email) throws Exception {
        return conteudo(email.getContent());
    }

    private static String conteudo(Object parte) throws Exception {
        if (parte instanceof Multipart multipart) {
            StringBuilder texto = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                texto.append(conteudo(multipart.getBodyPart(i).getContent())).append(System.lineSeparator());
            }
            return texto.toString();
        }
        return String.valueOf(parte);
    }

    private static Address[] recipients(MimeMessage email) {
        try {
            return email.getAllRecipients();
        } catch (MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String situacao(long aviso) {
        return jdbc.queryForObject("SELECT situacao FROM aviso_de_alvo WHERE id = ?", String.class, aviso);
    }

    private int tentativas(long aviso) {
        return jdbc.queryForObject("SELECT tentativas FROM aviso_de_alvo WHERE id = ?", Integer.class, aviso);
    }

    private String email(long usuario) {
        return jdbc.queryForObject("SELECT email FROM usuario WHERE id = ?", String.class, usuario);
    }

    private long usuarioGoogle() {
        return registrarUsuario(jdbc.queryForObject("""
                INSERT INTO usuario (email, google_id, nome_exibicao) VALUES (?, ?, 'Pessoa') RETURNING id
                """, Long.class, UUID.randomUUID() + "@umbra.dev", UUID.randomUUID().toString()));
    }

    private long usuarioPorSenha() {
        return registrarUsuario(jdbc.queryForObject("""
                INSERT INTO usuario (email, senha_hash, nome_exibicao) VALUES (?, 'hash', 'Pessoa') RETURNING id
                """, Long.class, UUID.randomUUID() + "@umbra.dev"));
    }

    private long registrarUsuario(long id) {
        usuarios.add(id);
        return id;
    }

    private long monitoramento(long usuario, long produto, long alvo) {
        return jdbc.queryForObject("""
                INSERT INTO monitoramento (usuario_id, produto_id, preco_alvo_centavos) VALUES (?, ?, ?) RETURNING id
                """, Long.class, usuario, produto, alvo);
    }

    private long aviso(long monitoramento, String motivo, Instant registradoEm) {
        return jdbc.queryForObject("""
                INSERT INTO aviso_de_alvo (monitoramento_id, motivo, melhor_preco_centavos, registrado_em)
                VALUES (?, ?, 0, ?) RETURNING id
                """, Long.class, monitoramento, motivo, Timestamp.from(registradoEm));
    }

    private long produtoComPreco(String nome, String codigoLoja, long valor) {
        long produto = jdbc.queryForObject("INSERT INTO produto (tipo, nome, ativo) VALUES ('JOGO', ?, true) RETURNING id",
                Long.class, nome + " " + UUID.randomUUID());
        produtos.add(produto);
        String identificador = UUID.randomUUID().toString();
        long oferta = jdbc.queryForObject("""
                INSERT INTO oferta (produto_id, loja_id, identificador_loja, url, ativa)
                VALUES (?, (SELECT id FROM loja WHERE codigo = ?), ?, ?, true)
                RETURNING id
                """, Long.class, produto, codigoLoja, identificador, "https://loja.test/" + identificador);
        jdbc.update("""
                INSERT INTO preco (oferta_id, valor_centavos, valor_original_centavos, desconto_pct, disponivel,
                                   origem_coleta, coletado_em)
                VALUES (?, ?, ?, 0, true, 'ITAD_API', ?)
                """, oferta, valor, valor, Timestamp.from(agora().minus(Duration.ofMinutes(5))));
        return produto;
    }

    private static Instant agora() {
        return Instant.now();
    }
}
