CREATE TABLE monitoramento (
    id                   BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id           BIGINT      NOT NULL,
    produto_id           BIGINT      NOT NULL,
    preco_alvo_centavos  BIGINT      NOT NULL,
    ativo                BOOLEAN     NOT NULL DEFAULT TRUE,
    ultimo_aviso_em      TIMESTAMPTZ,
    criado_em            TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_monit_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_monit_produto FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE RESTRICT,
    CONSTRAINT uq_monit_usuario_produto UNIQUE (usuario_id, produto_id),
    CONSTRAINT ck_monit_alvo CHECK (preco_alvo_centavos > 0)
);

CREATE INDEX ix_monit_produto_ativo  ON monitoramento (produto_id) WHERE ativo;
CREATE INDEX ix_monit_usuario_criado ON monitoramento (usuario_id, criado_em DESC, id DESC);
