ALTER TABLE monitoramento
    ADD COLUMN preco_avisado_centavos BIGINT,
    ADD CONSTRAINT ck_monit_preco_avisado CHECK (preco_avisado_centavos >= 0);

CREATE TABLE aviso_de_alvo (
    id                     BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    monitoramento_id       BIGINT      NOT NULL,
    motivo                 VARCHAR(20) NOT NULL,
    melhor_preco_centavos  BIGINT      NOT NULL,
    registrado_em          TIMESTAMPTZ NOT NULL,
    enviado_em             TIMESTAMPTZ,
    CONSTRAINT fk_aviso_monitoramento FOREIGN KEY (monitoramento_id) REFERENCES monitoramento (id) ON DELETE CASCADE,
    CONSTRAINT ck_aviso_motivo CHECK (motivo IN ('ENTRADA', 'QUEDA')),
    CONSTRAINT ck_aviso_melhor_preco CHECK (melhor_preco_centavos >= 0)
);

CREATE INDEX ix_aviso_pendente ON aviso_de_alvo (registrado_em) WHERE enviado_em IS NULL;
CREATE INDEX ix_aviso_monitoramento ON aviso_de_alvo (monitoramento_id);

CREATE TABLE aviso_de_alvo_loja (
    aviso_id        BIGINT NOT NULL,
    oferta_id       BIGINT NOT NULL,
    valor_centavos  BIGINT NOT NULL,
    PRIMARY KEY (aviso_id, oferta_id),
    CONSTRAINT fk_aviso_loja_aviso FOREIGN KEY (aviso_id) REFERENCES aviso_de_alvo (id) ON DELETE CASCADE,
    CONSTRAINT fk_aviso_loja_oferta FOREIGN KEY (oferta_id) REFERENCES oferta (id) ON DELETE RESTRICT,
    CONSTRAINT ck_aviso_loja_valor CHECK (valor_centavos >= 0)
);
