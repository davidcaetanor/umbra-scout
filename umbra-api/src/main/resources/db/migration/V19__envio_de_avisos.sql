ALTER TABLE aviso_de_alvo
    ADD COLUMN situacao   VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN tentativas INTEGER     NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_aviso_situacao CHECK (situacao IN ('PENDENTE', 'ENVIADO', 'DESCARTADO', 'FALHOU')),
    ADD CONSTRAINT ck_aviso_tentativas CHECK (tentativas >= 0);

UPDATE aviso_de_alvo SET situacao = 'DESCARTADO' WHERE enviado_em IS NULL;
UPDATE aviso_de_alvo SET situacao = 'ENVIADO' WHERE enviado_em IS NOT NULL;

DROP INDEX ix_aviso_pendente;
CREATE INDEX ix_aviso_pendente ON aviso_de_alvo (monitoramento_id) WHERE situacao = 'PENDENTE';

CREATE TABLE email_de_aviso (
    id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL,
    enviado_em  TIMESTAMPTZ NOT NULL,
    itens       INTEGER     NOT NULL,
    CONSTRAINT fk_email_aviso_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT ck_email_aviso_itens CHECK (itens > 0)
);

CREATE INDEX ix_email_aviso_usuario ON email_de_aviso (usuario_id, enviado_em);
