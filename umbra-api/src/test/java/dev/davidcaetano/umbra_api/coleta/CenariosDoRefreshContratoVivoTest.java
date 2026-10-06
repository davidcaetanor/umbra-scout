package dev.davidcaetano.umbra_api.coleta;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.coleta.itad.ItadClient;
import dev.davidcaetano.umbra_api.coleta.itad.ItadScraper;
import dev.davidcaetano.umbra_api.coleta.itad.LojaItad;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadDescobertaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoDescobertoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadOfertaPrecoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadPrecoJogoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadValorResponse;
import dev.davidcaetano.umbra_api.coleta.service.ColetaService;
import dev.davidcaetano.umbra_api.coleta.service.VereditoRodada;
import dev.davidcaetano.umbra_api.coleta.steam.SteamScraper;
import dev.davidcaetano.umbra_api.comum.IntegrationTestBase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("contrato-vivo")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class CenariosDoRefreshContratoVivoTest extends IntegrationTestBase {

    private static final int SHOP_STEAM = 61;
    private static final int PAGINAS_DA_AMOSTRA = 3;
    private static final int TAMANHO_LOTE = 200;
    private static final int LIMITE_EXEMPLOS = 10;
    private static final int AMOSTRA_DE_FORMATO = 5;
    private static final String COUNTRY = "BR";
    private static final String MOEDA_ESPERADA = "BRL";
    private static final String PREFIXO_APP = "app/";
    private static final String PREFIXO_SUB = "sub/";
    private static final List<String> IDENTIFICADORES_GRATUITOS =
            List.of("app/730", "app/570", "app/440", "app/1172470", "app/578080", "app/230410");

    private static final String MESMO_GID = "mesmoGid";
    private static final String OUTRO_GID = "outroGid";
    private static final String NULO = "nulo";
    private static final String AUSENTE_NA_RESPOSTA = "ausenteNaResposta";

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA_GENERICO =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA_GENERICA =
            new ParameterizedTypeReference<>() {
            };

    @DynamicPropertySource
    static void apontarParaApisReais(DynamicPropertyRegistry registry) {
        registry.add("itad.itad-api.base-url", () -> "https://api.isthereanydeal.com");
        registry.add("steam.steam-api.base-url", () -> "https://store.steampowered.com/api");
        // Mesmo motivo do ItadContratoVivoTest: vazia derruba a subida no @NotBlank, null cairia na chave fake.
        registry.add("itad.itad-api.api-key", () -> Objects.requireNonNullElse(System.getenv("ITAD_API_KEY"), ""));
    }

    @Autowired
    private ItadClient itadClient;

    @Autowired
    private ItadScraper itadScraper;

    @Autowired
    private SteamScraper steamScraper;

    @Autowired
    private ColetaService coletaService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    @Qualifier("itadRestClient")
    private RestClient itadRestClient;

    @Test
    void amostraDoItad_deveMedirIdentificadoresLookupReversoEPrecos() {
        List<ItadJogoDescobertoResponse> descobertos = buscarDescobertaDaAmostra();
        List<ItadJogoDescobertoResponse> jogos = descobertos.stream()
                .filter(item -> "game".equals(item.type()))
                .toList();
        List<UUID> gids = jogos.stream()
                .map(ItadJogoDescobertoResponse::id)
                .distinct()
                .toList();

        System.out.println("[amostra] itens=" + descobertos.size() + " jogos=" + jogos.size() + " gids=" + gids.size());

        assertThat(gids).isNotEmpty();

        List<ItadPrecoJogoResponse> respostaA = buscarPrecosEmLotes(gids);
        Instant instanteDaRespostaA = Instant.now();

        medirRespostaDePrecos(gids, respostaA);
        Map<UUID, List<String>> identificadoresPorGid = medirIdentificadoresDaSteam(jogos, respostaA);
        medirLookupReverso(identificadoresPorGid);
        medirEstabilidadeEntreChamadas(jogos, gids, respostaA, instanteDaRespostaA);
    }

    @Test
    void rodadasReais_deveMedirAlarmeCasamentoEntreFontesEGidFaltante() {
        assertThat(contar("SELECT count(*) FROM produto WHERE tipo = 'JOGO'")).isZero();

        ResultadoColeta rodadaSteam = coletarMedindo("[cenario-6]", steamScraper);
        ResultadoColeta rodadaItad = coletarMedindo("[cenario-5]", itadScraper);

        gravarMedindo("[cenario-5]", rodadaItad);
        long produtosJogoAposItad = medirCasamentoAposItad();

        Map<String, Long> ofertasSteamQueJaExistiam = medirRodadaSteamContraBanco(rodadaSteam);
        gravarMedindo("[cenario-6]", rodadaSteam);
        medirCasamentoAposSteam(produtosJogoAposItad, ofertasSteamQueJaExistiam);

        medirGidFaltante();
    }

    // ---- cenário 4: o que o /games/prices/v3 devolve ----

    private void medirRespostaDePrecos(List<UUID> gidsPedidos, List<ItadPrecoJogoResponse> respostaA) {
        Set<UUID> gidsDevolvidos = respostaA.stream()
                .map(ItadPrecoJogoResponse::id)
                .collect(Collectors.toSet());
        long naoVoltaram = gidsPedidos.stream().filter(gid -> !gidsDevolvidos.contains(gid)).count();

        System.out.println("[cenario-4] gidsPedidos=" + gidsPedidos.size()
                + " gidsDevolvidos=" + gidsDevolvidos.size()
                + " naoVoltaram=" + naoVoltaram
                + " pctNaoVoltaram=" + pct(naoVoltaram, gidsPedidos.size()));

        List<ItadOfertaPrecoResponse> ofertas = todasAsOfertas(respostaA);
        List<Integer> shopsSuportados = LojaItad.shopIds();

        Map<Integer, Long> porLoja = ofertas.stream()
                .filter(oferta -> oferta.shop() != null)
                .collect(Collectors.groupingBy(oferta -> oferta.shop().id(), TreeMap::new, Collectors.counting()));

        long paresComMaisDeUmaOferta = respostaA.stream()
                .mapToLong(jogo -> jogo.deals().stream()
                        .filter(oferta -> oferta.shop() != null)
                        .collect(Collectors.groupingBy(oferta -> oferta.shop().id(), Collectors.counting()))
                        .values().stream()
                        .filter(quantidade -> quantidade > 1)
                        .count())
                .sum();

        System.out.println("[cenario-4] ofertas=" + ofertas.size()
                + " porLoja=" + porLoja
                + " cutZero=" + ofertas.stream().filter(oferta -> oferta.cut() == 0).count()
                + " precoZero=" + ofertas.stream().filter(oferta -> valorZero(oferta.price())).count()
                + " regularZero=" + ofertas.stream().filter(oferta -> valorZero(oferta.regular())).count()
                + " precoZeroComRegularPositivo=" + ofertas.stream()
                .filter(oferta -> valorZero(oferta.price()) && valorPositivo(oferta.regular())).count()
                + " moedaNaoBRL=" + ofertas.stream()
                .filter(oferta -> !MOEDA_ESPERADA.equals(moeda(oferta.price()))
                        || !MOEDA_ESPERADA.equals(moeda(oferta.regular()))).count()
                + " lojaForaDasCinco=" + ofertas.stream()
                .filter(oferta -> oferta.shop() == null || !shopsSuportados.contains(oferta.shop().id())).count()
                + " paresComMaisDeUmaOferta=" + paresComMaisDeUmaOferta);

        medirVariantesDePrecos(particionar(gidsPedidos, TAMANHO_LOTE).getFirst());
        medirJogosGratuitos();
    }

    private void medirVariantesDePrecos(List<UUID> primeiroLote) {
        imprimirVariante("semParametro", buscarPrecosCrus(primeiroLote, Map.of()));
        imprimirVariante("deals=false", buscarPrecosCrus(primeiroLote, Map.of("deals", false)));
        imprimirVariante("deals=true", buscarPrecosCrus(primeiroLote, Map.of("deals", true)));
        imprimirVariante("vouchers=false", buscarPrecosCrus(primeiroLote, Map.of("vouchers", false)));
    }

    private static void imprimirVariante(String variante, List<Map<String, Object>> resposta) {
        List<Map<String, Object>> ofertas = resposta.stream()
                .flatMap(jogo -> ofertasCruas(jogo).stream())
                .toList();

        System.out.println("[cenario-4] variante=" + variante
                + " ofertas=" + ofertas.size()
                + " cutZero=" + ofertas.stream()
                .filter(oferta -> oferta.get("cut") instanceof Number cut && cut.intValue() == 0).count()
                + " comVoucher=" + ofertas.stream().filter(oferta -> oferta.get("voucher") != null).count());
    }

    private void medirJogosGratuitos() {
        Map<String, Object> gidPorIdentificador = lookupReverso(IDENTIFICADORES_GRATUITOS);

        List<UUID> gidsResolvidos = IDENTIFICADORES_GRATUITOS.stream()
                .map(gidPorIdentificador::get)
                .filter(Objects::nonNull)
                .map(gid -> UUID.fromString(gid.toString()))
                .distinct()
                .toList();

        Map<UUID, ItadPrecoJogoResponse> precoPorGid = gidsResolvidos.isEmpty() ? Map.of()
                : itadClient.buscarPrecos(gidsResolvidos).stream()
                .collect(Collectors.toMap(ItadPrecoJogoResponse::id, Function.identity(), (primeiro, segundo) -> primeiro));

        for (String identificador : IDENTIFICADORES_GRATUITOS) {
            Object gid = gidPorIdentificador.get(identificador);
            ItadPrecoJogoResponse preco = gid == null ? null : precoPorGid.get(UUID.fromString(gid.toString()));
            Optional<ItadOfertaPrecoResponse> ofertaSteam = Optional.ofNullable(preco).stream()
                    .flatMap(jogo -> jogo.deals().stream())
                    .filter(CenariosDoRefreshContratoVivoTest::ehDaSteam)
                    .findFirst();

            System.out.println("[cenario-4] gratuito id=" + identificador
                    + " gid=" + gid
                    + " voltouNoPreco=" + (preco != null)
                    + " steamPrice=" + ofertaSteam.map(oferta -> valor(oferta.price())).orElse(null)
                    + " steamRegular=" + ofertaSteam.map(oferta -> valor(oferta.regular())).orElse(null)
                    + " steamCut=" + ofertaSteam.map(ItadOfertaPrecoResponse::cut).orElse(null));
        }
    }

    // ---- cenário 1: identificadores da Steam por jogo ----

    private Map<UUID, List<String>> medirIdentificadoresDaSteam(List<ItadJogoDescobertoResponse> jogos,
                                                                 List<ItadPrecoJogoResponse> respostaA) {
        List<UUID> gidsComOfertaSteam = respostaA.stream()
                .filter(jogo -> jogo.deals().stream().anyMatch(CenariosDoRefreshContratoVivoTest::ehDaSteam))
                .map(ItadPrecoJogoResponse::id)
                .distinct()
                .toList();

        Map<UUID, List<String>> resolvido = new LinkedHashMap<>();
        for (List<UUID> lote : particionar(gidsComOfertaSteam, TAMANHO_LOTE)) {
            resolvido.putAll(itadClient.resolverIdentificadorNativo(SHOP_STEAM, lote));
        }

        Map<UUID, List<String>> identificadoresPorGid = new LinkedHashMap<>();
        gidsComOfertaSteam.forEach(gid ->
                identificadoresPorGid.put(gid, Objects.requireNonNullElse(resolvido.get(gid), List.of())));

        int semIdentificador = 0;
        int soSub = 0;
        int umApp = 0;
        List<UUID> gidsComDoisOuMaisApp = new ArrayList<>();
        Map<String, Long> outrosPrefixos = new TreeMap<>();

        for (Map.Entry<UUID, List<String>> entrada : identificadoresPorGid.entrySet()) {
            List<String> identificadores = entrada.getValue();
            long apps = identificadores.stream().filter(id -> id.startsWith(PREFIXO_APP)).count();

            if (identificadores.isEmpty()) {
                semIdentificador++;
            } else if (apps >= 2) {
                gidsComDoisOuMaisApp.add(entrada.getKey());
            } else if (apps == 1) {
                umApp++;
            } else if (identificadores.stream().allMatch(id -> id.startsWith(PREFIXO_SUB))) {
                soSub++;
            }

            identificadores.stream()
                    .filter(id -> !id.startsWith(PREFIXO_APP) && !id.startsWith(PREFIXO_SUB))
                    .forEach(id -> outrosPrefixos.merge(prefixo(id), 1L, Long::sum));
        }

        System.out.println("[cenario-1] gidsComOfertaSteam=" + gidsComOfertaSteam.size()
                + " semIdentificador=" + semIdentificador
                + " soSub=" + soSub
                + " umApp=" + umApp
                + " doisOuMaisApp=" + gidsComDoisOuMaisApp.size()
                + " outrosPrefixos=" + outrosPrefixos);

        Map<UUID, String> tituloPorGid = new LinkedHashMap<>();
        jogos.forEach(jogo -> tituloPorGid.putIfAbsent(jogo.id(), jogo.title()));

        gidsComDoisOuMaisApp.stream().limit(LIMITE_EXEMPLOS).forEach(gid ->
                System.out.println("[cenario-1] exemplo gid=" + gid
                        + " titulo=" + tituloPorGid.get(gid)
                        + " identificadores=" + identificadoresPorGid.get(gid)));

        return identificadoresPorGid;
    }

    // ---- cenário 2: lookup reverso ----

    private void medirLookupReverso(Map<UUID, List<String>> identificadoresPorGid) {
        Map<String, UUID> gidEsperadoPorIdentificador = new LinkedHashMap<>();
        identificadoresPorGid.forEach((gid, identificadores) ->
                identificadores.forEach(id -> gidEsperadoPorIdentificador.putIfAbsent(id, gid)));

        List<String> apps = comPrefixo(gidEsperadoPorIdentificador.keySet(), PREFIXO_APP);
        Map<String, Object> resposta = lookupReverso(apps);

        System.out.println("[cenario-2] forma chavesDeExemplo=" + resposta.keySet().stream().limit(AMOSTRA_DE_FORMATO).toList()
                + " tiposDeValor=" + contarTiposDeValor(resposta));

        Map<String, Long> situacoes = classificarLookupReverso(apps, resposta, gidEsperadoPorIdentificador::get);
        System.out.println("[cenario-2] enviados=" + apps.size()
                + " mesmoGid=" + situacoes.get(MESMO_GID)
                + " outroGid=" + situacoes.get(OUTRO_GID)
                + " nulo=" + situacoes.get(NULO)
                + " ausenteNaResposta=" + situacoes.get(AUSENTE_NA_RESPOSTA));

        apps.stream()
                .filter(id -> {
                    String situacao = situacaoNoLookup(resposta, id, gidEsperadoPorIdentificador.get(id));
                    return OUTRO_GID.equals(situacao) || NULO.equals(situacao);
                })
                .limit(LIMITE_EXEMPLOS)
                .forEach(id -> System.out.println("[cenario-2] exemplo id=" + id
                        + " gidEsperado=" + gidEsperadoPorIdentificador.get(id)
                        + " gidDevolvido=" + resposta.get(id)));

        List<String> soDigitos = apps.stream()
                .limit(AMOSTRA_DE_FORMATO)
                .map(id -> id.substring(PREFIXO_APP.length()))
                .toList();
        imprimirFormatoDoLookup("soDigitos", soDigitos,
                digitos -> gidEsperadoPorIdentificador.get(PREFIXO_APP + digitos));

        List<String> subs = comPrefixo(gidEsperadoPorIdentificador.keySet(), PREFIXO_SUB).stream()
                .limit(AMOSTRA_DE_FORMATO)
                .toList();
        imprimirFormatoDoLookup("sub", subs, gidEsperadoPorIdentificador::get);
    }

    private void imprimirFormatoDoLookup(String formato, List<String> enviados, Function<String, UUID> gidEsperado) {
        Map<String, Long> situacoes = classificarLookupReverso(enviados, lookupReverso(enviados), gidEsperado);

        System.out.println("[cenario-2] formato=" + formato
                + " enviados=" + enviados.size()
                + " mesmoGid=" + situacoes.get(MESMO_GID)
                + " outroGid=" + situacoes.get(OUTRO_GID)
                + " nulo=" + situacoes.get(NULO)
                + " ausenteNaResposta=" + situacoes.get(AUSENTE_NA_RESPOSTA));
    }

    private static Map<String, Long> classificarLookupReverso(List<String> enviados, Map<String, Object> resposta,
                                                              Function<String, UUID> gidEsperado) {
        Map<String, Long> situacoes = new LinkedHashMap<>();
        List.of(MESMO_GID, OUTRO_GID, NULO, AUSENTE_NA_RESPOSTA).forEach(situacao -> situacoes.put(situacao, 0L));
        enviados.forEach(id -> situacoes.merge(situacaoNoLookup(resposta, id, gidEsperado.apply(id)), 1L, Long::sum));
        return situacoes;
    }

    private static String situacaoNoLookup(Map<String, Object> resposta, String enviado, UUID gidEsperado) {
        if (!resposta.containsKey(enviado)) {
            return AUSENTE_NA_RESPOSTA;
        }
        Object devolvido = resposta.get(enviado);
        if (devolvido == null) {
            return NULO;
        }
        return devolvido.toString().equalsIgnoreCase(String.valueOf(gidEsperado)) ? MESMO_GID : OUTRO_GID;
    }

    private static Map<String, Long> contarTiposDeValor(Map<String, Object> resposta) {
        return resposta.values().stream()
                .collect(Collectors.groupingBy(
                        valor -> valor == null ? "null" : valor.getClass().getSimpleName(),
                        TreeMap::new,
                        Collectors.counting()));
    }

    // ---- cenário 3: estabilidade entre duas chamadas ----

    private void medirEstabilidadeEntreChamadas(List<ItadJogoDescobertoResponse> jogos, List<UUID> gids,
                                                List<ItadPrecoJogoResponse> respostaA, Instant instanteDaRespostaA) {
        List<ItadPrecoJogoResponse> respostaB = buscarPrecosEmLotes(gids);
        Instant instanteDaRespostaB = Instant.now();

        Map<ParGidLoja, ItadOfertaPrecoResponse> ofertaPorParA = primeiraOfertaPorPar(respostaA);
        Map<ParGidLoja, ItadOfertaPrecoResponse> ofertaPorParB = primeiraOfertaPorPar(respostaB);

        Set<ParGidLoja> todosOsPares = new HashSet<>(ofertaPorParA.keySet());
        todosOsPares.addAll(ofertaPorParB.keySet());

        List<ParGidLoja> paresEmComum = ofertaPorParA.keySet().stream()
                .filter(ofertaPorParB::containsKey)
                .toList();

        System.out.println("[cenario-3] intervaloSegundos=" + segundos(Duration.between(instanteDaRespostaA, instanteDaRespostaB))
                + " pares=" + todosOsPares.size()
                + " soNaPrimeira=" + ofertaPorParA.keySet().stream().filter(par -> !ofertaPorParB.containsKey(par)).count()
                + " soNaSegunda=" + ofertaPorParB.keySet().stream().filter(par -> !ofertaPorParA.containsKey(par)).count()
                + " precoDiferente=" + contarDiferencas(paresEmComum, ofertaPorParA, ofertaPorParB, oferta -> valor(oferta.price()))
                + " regularDiferente=" + contarDiferencas(paresEmComum, ofertaPorParA, ofertaPorParB, oferta -> valor(oferta.regular()))
                + " urlDiferente=" + contarDiferencas(paresEmComum, ofertaPorParA, ofertaPorParB, ItadOfertaPrecoResponse::url));

        List<ItadOfertaPrecoResponse> ofertasA = todasAsOfertas(respostaA);

        System.out.println("[cenario-3] hostsDaUrl=" + ofertasA.stream()
                .collect(Collectors.groupingBy(oferta -> host(oferta.url()), TreeMap::new, Collectors.counting())));

        ofertasA.stream()
                .map(ItadOfertaPrecoResponse::url)
                .distinct()
                .limit(3)
                .forEach(url -> System.out.println("[cenario-3] exemploUrl=" + url));

        Map<ParGidLoja, String> urlDaDescobertaPorPar = new LinkedHashMap<>();
        jogos.stream()
                .filter(jogo -> jogo.deal() != null && jogo.deal().shop() != null)
                .forEach(jogo -> urlDaDescobertaPorPar.putIfAbsent(
                        new ParGidLoja(jogo.id(), jogo.deal().shop().id()), jogo.deal().url()));

        List<Map.Entry<ParGidLoja, String>> comparaveis = urlDaDescobertaPorPar.entrySet().stream()
                .filter(entrada -> ofertaPorParA.containsKey(entrada.getKey()))
                .toList();

        System.out.println("[cenario-3] urlDaDescobertaIgualADePrecos=" + comparaveis.stream()
                .filter(entrada -> Objects.equals(entrada.getValue(), ofertaPorParA.get(entrada.getKey()).url()))
                .count()
                + " de=" + comparaveis.size());

        List<OffsetDateTime> timestamps = ofertasA.stream()
                .map(ItadOfertaPrecoResponse::timestamp)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        System.out.println("[cenario-3] timestampMaisAntigo=" + (timestamps.isEmpty() ? null : timestamps.getFirst())
                + " timestampMaisRecente=" + (timestamps.isEmpty() ? null : timestamps.getLast()));
    }

    private static Map<ParGidLoja, ItadOfertaPrecoResponse> primeiraOfertaPorPar(List<ItadPrecoJogoResponse> resposta) {
        Map<ParGidLoja, ItadOfertaPrecoResponse> ofertaPorPar = new LinkedHashMap<>();
        resposta.forEach(jogo -> jogo.deals().stream()
                .filter(oferta -> oferta.shop() != null)
                .forEach(oferta -> ofertaPorPar.putIfAbsent(new ParGidLoja(jogo.id(), oferta.shop().id()), oferta)));
        return ofertaPorPar;
    }

    private static long contarDiferencas(List<ParGidLoja> pares,
                                         Map<ParGidLoja, ItadOfertaPrecoResponse> ofertaPorParA,
                                         Map<ParGidLoja, ItadOfertaPrecoResponse> ofertaPorParB,
                                         Function<ItadOfertaPrecoResponse, Object> campo) {
        return pares.stream()
                .filter(par -> !Objects.equals(campo.apply(ofertaPorParA.get(par)), campo.apply(ofertaPorParB.get(par))))
                .count();
    }

    // ---- cenários 5 e 6: rodadas reais e alarme ----

    private ResultadoColeta coletarMedindo(String prefixo, Scraper scraper) {
        long inicio = System.nanoTime();
        ResultadoColeta resultado = scraper.coletar();
        String segundosColeta = segundosDesde(inicio);

        Reconciliacao reconciliacao = resultado.reconciliacao();
        String porLoja = resultado.fonte() == OrigemColeta.ITAD_API
                ? " porLoja=" + resultado.ofertas().stream()
                .collect(Collectors.groupingBy(oferta -> oferta.loja().name(), TreeMap::new, Collectors.counting()))
                : "";

        System.out.println(prefixo + " fonte=" + resultado.fonte()
                + " segundosColeta=" + segundosColeta
                + " ofertas=" + resultado.ofertas().size()
                + porLoja
                + " elegiveis=" + resultado.totalElegivel()
                + " semPreco=" + resultado.totalSemPreco()
                + " pctSemPreco=" + pct(resultado.totalSemPreco(), resultado.totalElegivel())
                + " brutos=" + reconciliacao.brutos()
                + " distintos=" + reconciliacao.distintos()
                + " pctDuplicata=" + pct(reconciliacao.brutos() - reconciliacao.distintos(), reconciliacao.brutos()));

        return resultado;
    }

    private void gravarMedindo(String prefixo, ResultadoColeta resultado) {
        long inicio = System.nanoTime();
        VereditoRodada veredito = coletaService.gravar(resultado);

        System.out.println(prefixo + " fonte=" + resultado.fonte()
                + " situacao=" + veredito.situacao()
                + " condicoes=" + veredito.condicoes()
                + " segundosGravacao=" + segundosDesde(inicio));
    }

    // ---- cenário 7: casamento entre as fontes ----

    private long medirCasamentoAposItad() {
        long produtosJogo = contar("SELECT count(*) FROM produto WHERE tipo = 'JOGO'");

        System.out.println("[cenario-7] aposItad produtosJogo=" + produtosJogo
                + " comChaveItad=" + contar("SELECT count(*) FROM produto WHERE tipo = 'JOGO' AND chave_itad IS NOT NULL")
                + " ofertasPorLoja=" + ofertasPorLoja()
                + " linhasDePreco=" + contar("SELECT count(*) FROM preco"));

        Map<String, Object> formatos = jdbc.queryForMap("""
                SELECT count(*) FILTER (WHERE o.identificador_loja ~ '^[0-9]+$') AS so_digitos,
                       count(*) FILTER (WHERE o.identificador_loja LIKE 'sub/%')  AS sub,
                       count(*)                                                    AS total
                  FROM oferta o
                  JOIN loja l ON l.id = o.loja_id
                 WHERE l.codigo = 'STEAM'
                """);
        long soDigitos = ((Number) formatos.get("so_digitos")).longValue();
        long sub = ((Number) formatos.get("sub")).longValue();
        long total = ((Number) formatos.get("total")).longValue();

        System.out.println("[cenario-7] aposItad ofertasSteam soDigitos=" + soDigitos
                + " sub=" + sub
                + " outroFormato=" + (total - soDigitos - sub));

        System.out.println("[cenario-7] aposItad produtosComMaisDeUmaOfertaNaMesmaLoja=" + produtosComMaisDeUmaOfertaNaMesmaLoja());

        jdbc.queryForList("""
                        SELECT o.produto_id, l.codigo,
                               string_agg(o.identificador_loja, ', ' ORDER BY o.identificador_loja) AS identificadores
                          FROM oferta o
                          JOIN loja l ON l.id = o.loja_id
                         GROUP BY o.produto_id, l.codigo
                        HAVING count(*) > 1
                         ORDER BY o.produto_id
                         LIMIT ?
                        """, LIMITE_EXEMPLOS)
                .forEach(linha -> System.out.println("[cenario-7] exemploDuplicada produto=" + linha.get("produto_id")
                        + " loja=" + linha.get("codigo")
                        + " identificadores=[" + linha.get("identificadores") + "]"));

        return produtosJogo;
    }

    private Map<String, Long> medirRodadaSteamContraBanco(ResultadoColeta rodadaSteam) {
        Map<String, Long> ofertaSteamPorIdentificador = new LinkedHashMap<>();
        jdbc.query("""
                SELECT o.identificador_loja, o.id
                  FROM oferta o
                  JOIN loja l ON l.id = o.loja_id
                 WHERE l.codigo = 'STEAM'
                """, linha -> {
            ofertaSteamPorIdentificador.put(linha.getString("identificador_loja"), linha.getLong("id"));
        });

        List<String> identificadoresDaRodada = rodadaSteam.ofertas().stream()
                .map(OfertaColetada::identificadorLoja)
                .toList();
        long jaExistiamNoBanco = identificadoresDaRodada.stream().filter(ofertaSteamPorIdentificador::containsKey).count();

        System.out.println("[cenario-7] rodadaSteam ofertas=" + identificadoresDaRodada.size()
                + " jaExistiamNoBanco=" + jaExistiamNoBanco
                + " novas=" + (identificadoresDaRodada.size() - jaExistiamNoBanco));

        Map<String, Long> ofertasQueJaExistiam = new LinkedHashMap<>();
        identificadoresDaRodada.stream()
                .filter(ofertaSteamPorIdentificador::containsKey)
                .forEach(id -> ofertasQueJaExistiam.putIfAbsent(id, ofertaSteamPorIdentificador.get(id)));
        return ofertasQueJaExistiam;
    }

    private void medirCasamentoAposSteam(long produtosJogoAposItad, Map<String, Long> ofertasQueJaExistiam) {
        long produtosJogo = contar("SELECT count(*) FROM produto WHERE tipo = 'JOGO'");

        System.out.println("[cenario-7] aposSteam produtosJogo=" + produtosJogo
                + " semChaveItad=" + contar("SELECT count(*) FROM produto WHERE tipo = 'JOGO' AND chave_itad IS NULL")
                + " produtosCriadosPelaSteam=" + (produtosJogo - produtosJogoAposItad));

        Set<Long> ofertasComPrecoDaSteam = new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT oferta_id FROM preco WHERE origem_coleta = 'STEAM_API'", Long.class));

        List<Map.Entry<String, Long>> ganharamLinhaDePrecoDaSteam = ofertasQueJaExistiam.entrySet().stream()
                .filter(entrada -> ofertasComPrecoDaSteam.contains(entrada.getValue()))
                .toList();

        System.out.println("[cenario-7] aposSteam ofertasQueJaExistiam=" + ofertasQueJaExistiam.size()
                + " ganharamLinhaDePrecoDaSteam=" + ganharamLinhaDePrecoDaSteam.size());

        ganharamLinhaDePrecoDaSteam.stream()
                .limit(LIMITE_EXEMPLOS)
                .forEach(entrada -> System.out.println("[cenario-7] exemploPrecoDiferente id=" + entrada.getKey()
                        + " valorItad=" + ultimoValor(entrada.getValue(), OrigemColeta.ITAD_API)
                        + " valorSteam=" + ultimoValor(entrada.getValue(), OrigemColeta.STEAM_API)));

        System.out.println("[cenario-7] aposSteam produtosComMaisDeUmaOfertaNaMesmaLoja=" + produtosComMaisDeUmaOfertaNaMesmaLoja());
    }

    private Map<String, Long> ofertasPorLoja() {
        return contarPorChave("""
                SELECT l.codigo AS chave, count(*) AS total
                  FROM oferta o
                  JOIN loja l ON l.id = o.loja_id
                 GROUP BY l.codigo
                """);
    }

    private Map<String, Long> produtosComMaisDeUmaOfertaNaMesmaLoja() {
        Map<String, Long> porLoja = new TreeMap<>();
        ofertasPorLoja().keySet().forEach(codigo -> porLoja.put(codigo, 0L));
        porLoja.putAll(contarPorChave("""
                SELECT codigo AS chave, count(*) AS total
                  FROM (SELECT l.codigo, o.produto_id
                          FROM oferta o
                          JOIN loja l ON l.id = o.loja_id
                         GROUP BY l.codigo, o.produto_id
                        HAVING count(*) > 1) duplicadas
                 GROUP BY codigo
                """));
        return porLoja;
    }

    private Long ultimoValor(Long ofertaId, OrigemColeta origem) {
        return jdbc.queryForList("""
                        SELECT valor_centavos
                          FROM preco
                         WHERE oferta_id = ? AND origem_coleta = ?
                         ORDER BY coletado_em DESC, id DESC
                         LIMIT 1
                        """, Long.class, ofertaId, origem.name())
                .stream()
                .findFirst()
                .orElse(null);
    }

    // ---- cenário 8: gid faltante ----

    private void medirGidFaltante() {
        long produtosSemGid = contar("SELECT count(*) FROM produto WHERE tipo = 'JOGO' AND chave_itad IS NULL");

        List<Map<String, Object>> ofertasSteamSemGid = jdbc.queryForList("""
                SELECT p.nome, o.identificador_loja
                  FROM produto p
                  JOIN oferta o ON o.produto_id = p.id
                  JOIN loja l ON l.id = o.loja_id
                 WHERE p.tipo = 'JOGO' AND p.chave_itad IS NULL AND l.codigo = 'STEAM'
                 ORDER BY p.id
                """);

        List<String> enviados = ofertasSteamSemGid.stream()
                .map(linha -> PREFIXO_APP + linha.get("identificador_loja"))
                .toList();
        Map<String, Object> gidPorIdentificador = lookupReverso(enviados);

        Map<String, Map<String, Object>> donoPorGid = new LinkedHashMap<>();
        jdbc.queryForList("SELECT id, nome, chave_itad FROM produto WHERE chave_itad IS NOT NULL")
                .forEach(produto -> donoPorGid.put(gidNormalizado(produto.get("chave_itad")), produto));

        int resolveu = 0;
        int nulo = 0;
        int ausenteNaResposta = 0;
        List<String> conflitos = new ArrayList<>();

        for (Map<String, Object> ofertaSteam : ofertasSteamSemGid) {
            String enviado = PREFIXO_APP + ofertaSteam.get("identificador_loja");

            if (!gidPorIdentificador.containsKey(enviado)) {
                ausenteNaResposta++;
                continue;
            }
            Object gid = gidPorIdentificador.get(enviado);
            if (gid == null) {
                nulo++;
                continue;
            }
            resolveu++;

            Map<String, Object> dono = donoPorGid.get(gidNormalizado(gid));
            if (dono != null) {
                conflitos.add(descreverConflito(ofertaSteam, gid.toString(), dono));
            }
        }

        System.out.println("[cenario-8] produtosSemGid=" + produtosSemGid
                + " resolveu=" + resolveu
                + " nulo=" + nulo
                + " ausenteNaResposta=" + ausenteNaResposta
                + " gidJaTemDono=" + conflitos.size());

        conflitos.stream()
                .limit(LIMITE_EXEMPLOS)
                .forEach(System.out::println);
    }

    private String descreverConflito(Map<String, Object> ofertaSteam, String gid, Map<String, Object> dono) {
        List<String> ofertasSteamDoDono = jdbc.queryForList("""
                SELECT o.identificador_loja
                  FROM oferta o
                  JOIN loja l ON l.id = o.loja_id
                 WHERE l.codigo = 'STEAM' AND o.produto_id = ?
                 ORDER BY o.identificador_loja
                """, String.class, dono.get("id"));

        return "[cenario-8] conflito appid=" + ofertaSteam.get("identificador_loja")
                + " nomeProdutoSteam=" + ofertaSteam.get("nome")
                + " gid=" + gid
                + " nomeProdutoDono=" + dono.get("nome")
                + " ofertasSteamDoDono=" + ofertasSteamDoDono
                + " identificadoresDoGid=" + identificadoresDaSteam(UUID.fromString(gid));
    }

    private List<String> identificadoresDaSteam(UUID gid) {
        return itadClient.resolverIdentificadorNativo(SHOP_STEAM, List.of(gid)).get(gid);
    }

    // ---- chamadas e utilitários ----

    private List<ItadJogoDescobertoResponse> buscarDescobertaDaAmostra() {
        List<ItadJogoDescobertoResponse> itens = new ArrayList<>();
        int offset = 0;

        for (int pagina = 0; pagina < PAGINAS_DA_AMOSTRA; pagina++) {
            ItadDescobertaResponse resposta = itadClient.buscarDescoberta(offset);
            itens.addAll(resposta.list());
            if (!resposta.hasMore()) {
                break;
            }
            offset = resposta.nextOffset();
        }

        return itens;
    }

    private List<ItadPrecoJogoResponse> buscarPrecosEmLotes(List<UUID> gids) {
        List<ItadPrecoJogoResponse> precos = new ArrayList<>();
        for (List<UUID> lote : particionar(gids, TAMANHO_LOTE)) {
            precos.addAll(itadClient.buscarPrecos(lote));
        }
        return precos;
    }

    private List<Map<String, Object>> buscarPrecosCrus(List<UUID> gids, Map<String, Object> parametrosExtras) {
        String shops = LojaItad.shopIds().stream().map(String::valueOf).collect(Collectors.joining(","));

        return itadRestClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path("/games/prices/v3")
                            .queryParam("country", COUNTRY)
                            .queryParam("shops", shops);
                    parametrosExtras.forEach((nome, valor) -> uriBuilder.queryParam(nome, valor));
                    return uriBuilder.build();
                })
                .body(gids)
                .retrieve()
                .body(LISTA_GENERICA);
    }

    private Map<String, Object> lookupReverso(List<String> identificadores) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        for (List<String> lote : particionar(identificadores, TAMANHO_LOTE)) {
            Map<String, Object> parcial = itadRestClient.post()
                    .uri("/lookup/id/shop/{shopId}/v1", SHOP_STEAM)
                    .body(lote)
                    .retrieve()
                    .body(MAPA_GENERICO);
            if (parcial != null) {
                resposta.putAll(parcial);
            }
        }
        return resposta;
    }

    private long contar(String sql) {
        return Objects.requireNonNull(jdbc.queryForObject(sql, Long.class));
    }

    private Map<String, Long> contarPorChave(String sql) {
        Map<String, Long> contagem = new TreeMap<>();
        jdbc.query(sql, linha -> {
            contagem.put(linha.getString("chave"), linha.getLong("total"));
        });
        return contagem;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> ofertasCruas(Map<String, Object> jogo) {
        Object deals = jogo.get("deals");
        return deals == null ? List.of() : (List<Map<String, Object>>) deals;
    }

    private static List<ItadOfertaPrecoResponse> todasAsOfertas(List<ItadPrecoJogoResponse> resposta) {
        return resposta.stream().flatMap(jogo -> jogo.deals().stream()).toList();
    }

    private static List<String> comPrefixo(Collection<String> identificadores, String prefixo) {
        return identificadores.stream().filter(id -> id.startsWith(prefixo)).toList();
    }

    private static boolean ehDaSteam(ItadOfertaPrecoResponse oferta) {
        return oferta.shop() != null && oferta.shop().id() == SHOP_STEAM;
    }

    private static Integer valor(ItadValorResponse valor) {
        return valor == null ? null : valor.amountInt();
    }

    private static String moeda(ItadValorResponse valor) {
        return valor == null ? null : valor.currency();
    }

    private static boolean valorZero(ItadValorResponse valor) {
        return valor != null && valor.amountInt() == 0;
    }

    private static boolean valorPositivo(ItadValorResponse valor) {
        return valor != null && valor.amountInt() > 0;
    }

    private static String prefixo(String identificador) {
        int barra = identificador.indexOf('/');
        return barra < 0 ? "(sem prefixo)" : identificador.substring(0, barra);
    }

    private static String host(String url) {
        return url == null ? "null" : URI.create(url).getHost();
    }

    private static String gidNormalizado(Object gid) {
        return UUID.fromString(gid.toString()).toString();
    }

    private static String pct(long parte, long total) {
        return total == 0 ? "n/a" : String.format(Locale.ROOT, "%.2f", parte * 100.0 / total);
    }

    private static String segundosDesde(long inicioEmNanos) {
        return segundos(Duration.ofNanos(System.nanoTime() - inicioEmNanos));
    }

    private static String segundos(Duration duracao) {
        return String.format(Locale.ROOT, "%.1f", duracao.toMillis() / 1000.0);
    }

    private static <T> List<List<T>> particionar(List<T> lista, int tamanho) {
        List<List<T>> lotes = new ArrayList<>();
        for (int inicio = 0; inicio < lista.size(); inicio += tamanho) {
            lotes.add(lista.subList(inicio, Math.min(inicio + tamanho, lista.size())));
        }
        return lotes;
    }

    private record ParGidLoja(UUID gid, int shopId) {
    }
}
