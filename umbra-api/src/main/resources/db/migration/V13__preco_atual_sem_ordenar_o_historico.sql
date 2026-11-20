CREATE OR REPLACE VIEW vw_preco_atual AS
WITH ultimo AS (
    SELECT DISTINCT ON (p.oferta_id)
           p.oferta_id, p.id, p.valor_centavos, p.valor_original_centavos, p.desconto_pct,
           p.disponivel, p.expira_em, p.coletado_em
      FROM preco p
     ORDER BY p.oferta_id, p.coletado_em DESC, p.id DESC
),
oficial AS (
    SELECT DISTINCT ON (p.oferta_id)
           p.oferta_id, p.id, p.valor_centavos, p.valor_original_centavos, p.desconto_pct,
           p.disponivel, p.expira_em, p.coletado_em
      FROM preco p
      JOIN oferta o ON o.id = p.oferta_id
      JOIN loja l ON l.id = o.loja_id
      JOIN produto pr ON pr.id = o.produto_id
     WHERE p.coletado_em >= now() - INTERVAL '12 hours'
       AND p.origem_coleta = l.origem_autoritativa
       -- cadência do RF-01: 6 horas para JOGO, 12 horas para HARDWARE
       AND p.coletado_em >= now() - CASE pr.tipo
                                        WHEN 'JOGO' THEN INTERVAL '6 hours'
                                        WHEN 'HARDWARE' THEN INTERVAL '12 hours'
                                    END
     ORDER BY p.oferta_id, p.coletado_em DESC, p.id DESC
)
SELECT u.oferta_id,
       CASE WHEN f.id IS NULL THEN u.id ELSE f.id END AS preco_id,
       CASE WHEN f.id IS NULL THEN u.valor_centavos ELSE f.valor_centavos END AS valor_centavos,
       CASE WHEN f.id IS NULL THEN u.valor_original_centavos ELSE f.valor_original_centavos END AS valor_original_centavos,
       CASE WHEN f.id IS NULL THEN u.desconto_pct ELSE f.desconto_pct END AS desconto_pct,
       CASE WHEN f.id IS NULL THEN u.disponivel ELSE f.disponivel END AS disponivel,
       CASE WHEN f.id IS NULL THEN u.expira_em ELSE f.expira_em END AS expira_em,
       CASE WHEN f.id IS NULL THEN u.coletado_em ELSE f.coletado_em END AS coletado_em
  FROM ultimo u
  LEFT JOIN oficial f ON f.oferta_id = u.oferta_id;
