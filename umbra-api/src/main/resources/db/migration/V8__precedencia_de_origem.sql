ALTER TABLE loja ADD COLUMN origem_autoritativa VARCHAR(20);
ALTER TABLE loja ADD CONSTRAINT ck_loja_origem_autoritativa CHECK (origem_autoritativa IN
    ('STEAM_API', 'ITAD_API', 'NUUVEM_API', 'EPIC_API', 'TERABYTE_HTML', 'AWIN_FEED', 'KABUM_API'));

UPDATE loja SET origem_autoritativa = 'STEAM_API' WHERE codigo = 'STEAM';

CREATE OR REPLACE VIEW vw_preco_atual AS
SELECT DISTINCT ON (p.oferta_id)
       p.oferta_id, p.id AS preco_id, p.valor_centavos,
       p.valor_original_centavos, p.desconto_pct, p.disponivel,
       p.expira_em, p.coletado_em
  FROM preco p
  JOIN oferta o ON o.id = p.oferta_id
  JOIN loja l ON l.id = o.loja_id
  JOIN produto pr ON pr.id = o.produto_id
 ORDER BY p.oferta_id,
          -- cadência do RF-01: 6 horas para JOGO, 12 horas para HARDWARE
          COALESCE(p.origem_coleta = l.origem_autoritativa
                   AND p.coletado_em >= now() - CASE pr.tipo
                                                    WHEN 'JOGO' THEN INTERVAL '6 hours'
                                                    WHEN 'HARDWARE' THEN INTERVAL '12 hours'
                                                END, FALSE) DESC,
          p.coletado_em DESC, p.id DESC;
