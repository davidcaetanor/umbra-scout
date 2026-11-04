CREATE OR REPLACE VIEW vw_melhor_oferta_atual AS
SELECT DISTINCT ON (o.produto_id)
       o.produto_id, o.id AS oferta_id, o.loja_id, o.url,
       v.valor_centavos, v.valor_original_centavos, v.desconto_pct, v.coletado_em,
       v.expira_em
  FROM oferta o
  JOIN vw_preco_atual v ON v.oferta_id = o.id
 WHERE o.ativa AND v.disponivel
 ORDER BY o.produto_id, v.valor_centavos ASC, o.id;
