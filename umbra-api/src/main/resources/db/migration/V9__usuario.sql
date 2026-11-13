CREATE TABLE usuario (
    id             BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email          VARCHAR(320) NOT NULL,
    senha_hash     VARCHAR(72),
    discord_id     VARCHAR(32),
    google_id      VARCHAR(255),
    nome_exibicao  VARCHAR(60)  NOT NULL,
    cep            CHAR(8),
    cidade         VARCHAR(120),
    uf             CHAR(2),
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuario_discord    UNIQUE (discord_id),
    CONSTRAINT uq_usuario_google     UNIQUE (google_id),
    CONSTRAINT ck_usuario_credencial CHECK (senha_hash IS NOT NULL OR discord_id IS NOT NULL
                                            OR google_id IS NOT NULL),
    CONSTRAINT ck_usuario_email      CHECK (email = lower(email) AND position('@' IN email) > 1),
    CONSTRAINT ck_usuario_cep        CHECK (cep IS NULL OR cep ~ '^[0-9]{8}$'),
    CONSTRAINT ck_usuario_uf         CHECK (uf IS NULL OR uf ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_usuario_endereco   CHECK ((cidade IS NULL AND uf IS NULL) OR cep IS NOT NULL)
);

CREATE UNIQUE INDEX uq_usuario_email ON usuario (email);
