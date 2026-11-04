package dev.davidcaetano.umbra_api.comum.log;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ChaveDeLogTest {

    private static final String ARQUIVO_DE_LOG = "/logback-spring.xml";
    private static final Pattern CHAVE_DO_ENCODER_JSON =
            Pattern.compile("<includeMdcKeyName>\\s*([^<\\s]+)\\s*</includeMdcKeyName>");
    private static final Pattern PADRAO_DO_CONSOLE_LEGIVEL =
            Pattern.compile("<appender\\s+name=\"CONSOLE_LEGIVEL\".*?<pattern>(.*?)</pattern>", Pattern.DOTALL);

    private static String configuracao;

    @BeforeAll
    static void lerConfiguracaoDoLog() throws IOException {
        try (InputStream arquivo = ChaveDeLogTest.class.getResourceAsStream(ARQUIVO_DE_LOG)) {
            configuracao = new String(Objects.requireNonNull(arquivo, ARQUIVO_DE_LOG + " fora do classpath")
                    .readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void allowlistDoEncoderJsonDeveSerIgualAsChavesDoEnum() {
        Set<String> chavesDoEncoder = CHAVE_DO_ENCODER_JSON.matcher(configuracao).results()
                .map(resultado -> resultado.group(1))
                .collect(Collectors.toSet());

        assertThat(chavesDoEncoder).isEqualTo(chavesDoEnum());
    }

    @Test
    void padraoDoConsoleLegivelDeveMostrarCadaChaveDoEnum() {
        Matcher padrao = PADRAO_DO_CONSOLE_LEGIVEL.matcher(configuracao);
        assertThat(padrao.find()).as("appender CONSOLE_LEGIVEL com <pattern> em " + ARQUIVO_DE_LOG).isTrue();
        String formato = padrao.group(1);

        assertThat(chavesDoEnum()).allSatisfy(chave -> assertThat(formato).contains("%X{" + chave));
    }

    private static Set<String> chavesDoEnum() {
        return Arrays.stream(ChaveDeLog.values())
                .map(ChaveDeLog::chave)
                .collect(Collectors.toSet());
    }
}
