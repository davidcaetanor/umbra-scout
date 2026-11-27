CREATE TABLE qualidade_jogo (
    produto_id          BIGINT       PRIMARY KEY REFERENCES produto (id) ON DELETE CASCADE,
    steam_nota          SMALLINT     CHECK (steam_nota BETWEEN 0 AND 100),
    steam_avaliacoes    INTEGER      CHECK (steam_avaliacoes >= 0),
    critica_nota        SMALLINT     CHECK (critica_nota BETWEEN 0 AND 100),
    critica_avaliacoes  INTEGER      CHECK (critica_avaliacoes >= 0),
    lista_desejos       INTEGER      CHECK (lista_desejos >= 0),
    publicador          VARCHAR(200),
    consultada_em       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_qualidade_jogo_consultada_em ON qualidade_jogo (consultada_em);
