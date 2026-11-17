package dev.davidcaetano.umbra_api.comum.openapi;

public final class RespostaDeErro {

    public static final String PARAMETRO_INVALIDO = "ParametroInvalido";
    public static final String NAO_AUTENTICADO = "NaoAutenticado";
    public static final String NAO_ENCONTRADO = "NaoEncontrado";
    public static final String EM_CONFLITO = "EmConflito";
    public static final String REGRA_VIOLADA = "RegraViolada";
    public static final String SERVICO_INDISPONIVEL = "ServicoIndisponivel";
    public static final String ERRO_INTERNO = "ErroInterno";

    private static final String PREFIXO = "#/components/responses/";

    public static final String REF_PARAMETRO_INVALIDO = PREFIXO + PARAMETRO_INVALIDO;
    public static final String REF_NAO_AUTENTICADO = PREFIXO + NAO_AUTENTICADO;
    public static final String REF_NAO_ENCONTRADO = PREFIXO + NAO_ENCONTRADO;
    public static final String REF_EM_CONFLITO = PREFIXO + EM_CONFLITO;
    public static final String REF_REGRA_VIOLADA = PREFIXO + REGRA_VIOLADA;
    public static final String REF_SERVICO_INDISPONIVEL = PREFIXO + SERVICO_INDISPONIVEL;
    public static final String REF_ERRO_INTERNO = PREFIXO + ERRO_INTERNO;

    private RespostaDeErro() {
    }
}
