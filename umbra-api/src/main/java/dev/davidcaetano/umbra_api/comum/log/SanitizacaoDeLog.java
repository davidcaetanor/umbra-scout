package dev.davidcaetano.umbra_api.comum.log;

import java.util.regex.Pattern;

public final class SanitizacaoDeLog {

    static final int TAMANHO_MAXIMO = 256;

    private static final Pattern QUEBRA_DE_LINHA = Pattern.compile("\\r\\n|\\r|\\n|\\u2028|\\u2029|\\u0085");
    private static final Pattern FORA_DO_IMPRIMIVEL = Pattern.compile("[^\\x20-\\x7E]");

    private SanitizacaoDeLog() {
    }

    public static String paraLog(String valor) {
        if (valor == null) {
            return "";
        }
        String semQuebraDeLinha = QUEBRA_DE_LINHA.matcher(valor).replaceAll(" ");
        String truncado = semQuebraDeLinha.length() > TAMANHO_MAXIMO ? semQuebraDeLinha.substring(0, TAMANHO_MAXIMO) : semQuebraDeLinha;
        return FORA_DO_IMPRIMIVEL.matcher(truncado).replaceAll("");
    }
}
