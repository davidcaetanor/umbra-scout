package dev.davidcaetano.umbra_api.catalogo.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

final class ChaveDoJogo {

    private static final Pattern MARCAS = Pattern.compile("[\\u2122\\u00AE\\u00A9]");
    private static final Pattern ACENTOS = Pattern.compile("\\p{M}");
    private static final Pattern SUFIXO_DE_EDICAO = Pattern.compile(
            "\\s*(?:[:\\-\\u2013\\u2014]\\s*)?(?:the\\s+)?(?:(?:digital|super)\\s+)?"
                    + "(?:(?:ultimate|deluxe|goty|game\\s+of\\s+the\\s+year|director'?s\\s+cut)\\b"
                    + "|[\\p{L}\\p{N}+']+\\s+edition\\b|edition\\b|bundle\\b).*$");
    private static final Pattern PONTUACAO_NAS_PONTAS = Pattern.compile("^[\\s:\\-\\u2013\\u2014]+|[\\s:\\-\\u2013\\u2014]+$");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private ChaveDoJogo() {
    }

    static String de(String nome) {
        String semMarcas = MARCAS.matcher(nome).replaceAll("");
        String semAcentos = ACENTOS.matcher(Normalizer.normalize(semMarcas, Normalizer.Form.NFD)).replaceAll("");
        String minusculo = ESPACOS.matcher(semAcentos.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
        String semEdicao = SUFIXO_DE_EDICAO.matcher(minusculo).replaceFirst("");
        String chave = PONTUACAO_NAS_PONTAS.matcher(semEdicao).replaceAll("");
        return chave.isEmpty() ? minusculo : chave;
    }
}
