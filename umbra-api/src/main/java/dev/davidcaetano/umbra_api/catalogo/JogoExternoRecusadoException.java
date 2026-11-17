package dev.davidcaetano.umbra_api.catalogo;

import dev.davidcaetano.umbra_api.comum.excecao.RegraDeNegocioVioladaException;

public class JogoExternoRecusadoException extends RegraDeNegocioVioladaException {

    private JogoExternoRecusadoException(String detalhe) {
        super(detalhe);
    }

    public static JogoExternoRecusadoException naoEJogo(String gid) {
        return new JogoExternoRecusadoException("O item %s não é um jogo.".formatted(gid));
    }

    public static JogoExternoRecusadoException semOfertaNasLojasAcompanhadas(String gid) {
        return new JogoExternoRecusadoException(
                "O jogo %s não está à venda em nenhuma das lojas acompanhadas.".formatted(gid));
    }
}
