INSERT INTO loja (codigo, nome, url_base, ativa)
VALUES ('KABUM', 'KaBuM!', 'https://www.kabum.com.br', TRUE);

ALTER TABLE preco DROP CONSTRAINT ck_preco_origem;
ALTER TABLE preco ADD CONSTRAINT ck_preco_origem CHECK (origem_coleta IN
    ('STEAM_API', 'ITAD_API', 'NUUVEM_API', 'EPIC_API', 'TERABYTE_HTML', 'AWIN_FEED', 'KABUM_API'));
