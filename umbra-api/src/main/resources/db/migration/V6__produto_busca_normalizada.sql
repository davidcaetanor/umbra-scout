CREATE FUNCTION normalizar_busca(texto TEXT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    AS $$ SELECT lower(public.unaccent('public.unaccent'::regdictionary, texto)) $$;

CREATE INDEX ix_produto_nome_busca_trgm ON produto USING gin (normalizar_busca(nome) gin_trgm_ops);

DROP INDEX ix_produto_nome_trgm;
