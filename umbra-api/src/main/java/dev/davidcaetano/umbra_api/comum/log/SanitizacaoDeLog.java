package dev.davidcaetano.umbra_api.comum.log;

import java.util.regex.Pattern;

public final class SanitizacaoDeLog {

    static final int TAMANHO_MAXIMO = 256;

    private static final Pattern FORA_DO_IMPRIMIVEL = Pattern.compile("[^\\x20-\\x7E]");

    private SanitizacaoDeLog() {
    }

    public static String paraLog(String valor) {
        if (valor == null) {
            return "";
        }
        String truncado = valor.length() > TAMANHO_MAXIMO ? valor.substring(0, TAMANHO_MAXIMO) : valor;
        return FORA_DO_IMPRIMIVEL.matcher(truncado).replaceAll("");
    }
}
