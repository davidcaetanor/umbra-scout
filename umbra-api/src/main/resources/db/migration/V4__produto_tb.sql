DROP VIEW vw_preco_atual;

ALTER TABLE produto RENAME TO oferta;
ALTER TABLE oferta RENAME CONSTRAINT fk_produto_loja TO fk_oferta_loja;
ALTER TABLE oferta RENAME CONSTRAINT uq_produto_loja TO uq_oferta_loja;
ALTER TABLE oferta RENAME CONSTRAINT ck_produto_url  TO ck_oferta_url;
ALTER TABLE oferta DROP CONSTRAINT ck_produto_tipo;
ALTER TABLE oferta DROP CONSTRAINT ck_produto_nome;
ALTER TABLE oferta RENAME COLUMN ativo TO ativa;
DROP INDEX ix_produto_nome_trgm;
DROP INDEX ix_produto_tipo_cat;
DROP INDEX ix_produto_itad;

ALTER TABLE preco RENAME COLUMN produto_id TO oferta_id;
ALTER TABLE preco RENAME CONSTRAINT fk_preco_produto TO fk_preco_oferta;
ALTER INDEX ix_preco_produto_data RENAME TO ix_preco_oferta_data;

CREATE TABLE produto (
    id             BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo           VARCHAR(12)  NOT NULL,
    nome           VARCHAR(300) NOT NULL,
    categoria      VARCHAR(40),
    imagem_url     VARCHAR(600),
    chave_itad     VARCHAR(60),
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_produto_chave_itad UNIQUE (chave_itad),
    CONSTRAINT ck_produto_tipo CHECK (tipo IN ('JOGO','HARDWARE')),
    CONSTRAINT ck_produto_nome CHECK (length(btrim(nome)) > 0)
);

CREATE INDEX ix_produto_nome_trgm ON produto USING gin (nome gin_trgm_ops);
CREATE INDEX ix_produto_tipo_cat  ON produto (tipo, categoria) WHERE ativo;

ALTER TABLE oferta ADD COLUMN produto_id BIGINT NOT NULL;
ALTER TABLE oferta ADD CONSTRAINT fk_oferta_produto
    FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE RESTRICT;
CREATE INDEX ix_oferta_produto ON oferta (produto_id) WHERE ativa;

ALTER TABLE oferta
    DROP COLUMN nome,
    DROP COLUMN tipo,
    DROP COLUMN categoria,
    DROP COLUMN imagem_url,
    DROP COLUMN chave_itad;

CREATE VIEW vw_preco_atual AS
SELECT DISTINCT ON (p.oferta_id)
       p.oferta_id, p.id AS preco_id, p.valor_centavos,
       p.valor_original_centavos, p.desconto_pct, p.disponivel,
       p.expira_em, p.coletado_em
  FROM preco p
 ORDER BY p.oferta_id, p.coletado_em DESC, p.id DESC;

CREATE VIEW vw_melhor_oferta_atual AS
SELECT DISTINCT ON (o.produto_id)
       o.produto_id, o.id AS oferta_id, o.loja_id, o.url,
       v.valor_centavos, v.valor_original_centavos, v.desconto_pct, v.coletado_em
  FROM oferta o
  JOIN vw_preco_atual v ON v.oferta_id = o.id
 WHERE o.ativa AND v.disponivel
 ORDER BY o.produto_id, v.valor_centavos ASC, o.id;
