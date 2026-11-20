CREATE INDEX ix_preco_oferta_data_id ON preco (oferta_id, coletado_em DESC, id DESC);
DROP INDEX ix_preco_oferta_data;

CREATE INDEX ix_preco_oferta_valor_disponivel ON preco (oferta_id, valor_centavos) WHERE disponivel;

CREATE OR REPLACE VIEW vw_preco_atual AS
SELECT o.id AS oferta_id,
       CASE WHEN oficial.id IS NULL THEN ultimo.id ELSE oficial.id END AS preco_id,
       CASE WHEN oficial.id IS NULL THEN ultimo.valor_centavos ELSE oficial.valor_centavos END AS valor_centavos,
       CASE WHEN oficial.id IS NULL THEN ultimo.valor_original_centavos
            ELSE oficial.valor_original_centavos END AS valor_original_centavos,
       CASE WHEN oficial.id IS NULL THEN ultimo.desconto_pct ELSE oficial.desconto_pct END AS desconto_pct,
       CASE WHEN oficial.id IS NULL THEN ultimo.disponivel ELSE oficial.disponivel END AS disponivel,
       CASE WHEN oficial.id IS NULL THEN ultimo.expira_em ELSE oficial.expira_em END AS expira_em,
       CASE WHEN oficial.id IS NULL THEN ultimo.coletado_em ELSE oficial.coletado_em END AS coletado_em
  FROM oferta o
  JOIN loja l ON l.id = o.loja_id
  JOIN produto pr ON pr.id = o.produto_id
 CROSS JOIN LATERAL (SELECT p.*
                       FROM preco p
                      WHERE p.oferta_id = o.id
                      ORDER BY p.coletado_em DESC, p.id DESC
                      LIMIT 1) ultimo
  LEFT JOIN LATERAL (SELECT p.*
                       FROM preco p
                      WHERE l.origem_autoritativa IS NOT NULL
                        AND p.oferta_id = o.id
                        AND p.origem_coleta = l.origem_autoritativa
                        -- cadência do RF-01: 6 horas para JOGO, 12 horas para HARDWARE
                        AND p.coletado_em >= now() - CASE pr.tipo
                                                         WHEN 'JOGO' THEN INTERVAL '6 hours'
                                                         WHEN 'HARDWARE' THEN INTERVAL '12 hours'
                                                     END
                      ORDER BY p.coletado_em DESC, p.id DESC
                      LIMIT 1) oficial ON TRUE;
