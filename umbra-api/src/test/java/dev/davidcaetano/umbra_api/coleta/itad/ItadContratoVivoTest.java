package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoDescobertoResponse;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("contrato-vivo")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ItadContratoVivoTest extends IntegrationTestBase {

    private static final int SHOP_STEAM = 61;

    @DynamicPropertySource
    static void apontarParaApiReal(DynamicPropertyRegistry registry) {
        registry.add("itad.itad-api.base-url", () -> "https://api.isthereanydeal.com");
        // String vazia em vez de null: null cairia no valor fake do application-test.yaml;
        // vazia derruba a subida no @NotBlank de ItadProperties, que nomeia a chave.
        registry.add("itad.itad-api.api-key", () -> Objects.requireNonNullElse(System.getenv("ITAD_API_KEY"), ""));
    }

    @Autowired
    private ItadClient itadClient;

    @Test
    void lookupDaSteam_deveDevolverIdentificadorComPrefixoDeTipo() {
        List<UUID> gids = itadClient.buscarDescoberta(0).list().stream()
                .filter(jogo -> jogo.deal() != null && jogo.deal().shop() != null
                        && jogo.deal().shop().id() == SHOP_STEAM)
                .map(ItadJogoDescobertoResponse::id)
                .distinct()
                .toList();

        List<String> identificadores = itadClient.resolverIdentificadorNativo(SHOP_STEAM, gids).values().stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();

        Map<String, Long> contagemPorPrefixo = identificadores.stream()
                .collect(Collectors.groupingBy(ItadContratoVivoTest::prefixo, Collectors.counting()));

        System.out.println("[contrato-vivo] gids=" + gids.size()
                + " identificadores=" + identificadores.size()
                + " porPrefixo=" + contagemPorPrefixo
                + " exemplos=" + identificadores.stream().limit(5).toList());

        assertThat(identificadores).isNotEmpty();
        assertThat(identificadores).noneMatch(identificador -> identificador.matches("\\d+"));
        assertThat(identificadores).anyMatch(identificador -> identificador.startsWith("app/"));
    }

    private static String prefixo(String identificador) {
        int barra = identificador.indexOf('/');
        return barra < 0 ? "(sem prefixo)" : identificador.substring(0, barra);
    }
}
