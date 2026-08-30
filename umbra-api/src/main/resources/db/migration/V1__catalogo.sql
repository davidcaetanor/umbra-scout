CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE loja
(
    id             SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo         VARCHAR(20)  NOT NULL,
    nome           VARCHAR(60)  NOT NULL,
    url_base       VARCHAR(200) NOT NULL,
    param_afiliado VARCHAR(200),
    ativa          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_loja_codigo UNIQUE (codigo),
    CONSTRAINT ck_loja_codigo CHECK (codigo ~ '^[A-Z][A-Z0-9_]{2,19}$')
);

CREATE TABLE produto
(
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    loja_id            SMALLINT     NOT NULL,
    identificador_loja VARCHAR(120) NOT NULL,
    nome               VARCHAR(300) NOT NULL,
    tipo               VARCHAR(12)  NOT NULL,
    categoria          VARCHAR(40),
    url                VARCHAR(600) NOT NULL,
    imagem_url         VARCHAR(600),
    chave_itad         VARCHAR(60),
    ativo              BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_produto_loja FOREIGN KEY (loja_id) REFERENCES loja (id) ON DELETE RESTRICT,
    CONSTRAINT uq_produto_loja UNIQUE (loja_id, identificador_loja),
    CONSTRAINT ck_produto_tipo CHECK (tipo IN ('JOGO', 'HARDWARE')),
    CONSTRAINT ck_produto_url CHECK (url ~ '^https?://'),
    CONSTRAINT ck_produto_nome CHECK (length(btrim(nome)) > 0)
);

CREATE INDEX ix_produto_nome_trgm ON produto USING gin (nome gin_trgm_ops);
CREATE INDEX ix_produto_tipo_cat ON produto (tipo, categoria) WHERE ativo;
CREATE INDEX ix_produto_itad ON produto (chave_itad) WHERE chave_itad IS NOT NULL;

CREATE TABLE preco
(
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    produto_id              BIGINT      NOT NULL,
    valor_centavos          BIGINT      NOT NULL,
    valor_original_centavos BIGINT,
    desconto_pct            SMALLINT,
    disponivel              BOOLEAN     NOT NULL DEFAULT TRUE,
    origem_coleta           VARCHAR(20) NOT NULL,
    coletado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_preco_produto FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE RESTRICT,
    CONSTRAINT ck_preco_valor CHECK (valor_centavos >= 0),
    CONSTRAINT ck_preco_original CHECK (valor_original_centavos IS NULL
        OR valor_original_centavos >= valor_centavos),
    CONSTRAINT ck_preco_desconto CHECK (desconto_pct IS NULL OR desconto_pct BETWEEN 0 AND 100),
    CONSTRAINT ck_preco_origem CHECK (origem_coleta IN
                                      ('STEAM_API', 'ITAD_API', 'NUUVEM_API', 'EPIC_API', 'TERABYTE_HTML', 'AWIN_FEED'))
);

CREATE INDEX ix_preco_produto_data ON preco (produto_id, coletado_em DESC);

CREATE VIEW vw_preco_atual AS
SELECT DISTINCT ON (p.produto_id) p.produto_id,
                                  p.id AS preco_id,
                                  p.valor_centavos,
                                  p.valor_original_centavos,
                                  p.desconto_pct,
                                  p.disponivel,
                                  p.coletado_em
FROM preco p
ORDER BY p.produto_id, p.coletado_em DESC, p.id DESC;

INSERT INTO loja (codigo, nome, url_base, param_afiliado)
VALUES ('STEAM', 'Steam', 'https://store.steampowered.com', NULL),
       ('NUUVEM', 'Nuuvem', 'https://www.nuuvem.com', NULL),
       ('EPIC', 'Epic Games Store', 'https://store.epicgames.com', NULL),
       ('TERABYTE', 'Terabyte Shop', 'https://www.terabyteshop.com.br', NULL);
