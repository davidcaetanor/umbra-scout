CREATE MATERIALIZED VIEW mv_vitrine_ofertas AS
SELECT mo.produto_id, mo.oferta_id, mo.loja_id, mo.url,
       mo.valor_centavos, mo.valor_original_centavos, mo.desconto_pct,
       mo.expira_em, mo.coletado_em, o.verificado_em,
       minimo.valor_centavos AS minimo_centavos
  FROM vw_melhor_oferta_atual mo
  JOIN oferta o ON o.id = mo.oferta_id
  LEFT JOIN (SELECT om.produto_id, min(p.valor_centavos) AS valor_centavos
               FROM preco p
               JOIN oferta om ON om.id = p.oferta_id
              WHERE om.ativa AND p.disponivel
              GROUP BY om.produto_id) minimo ON minimo.produto_id = mo.produto_id;

CREATE UNIQUE INDEX ux_mv_vitrine_ofertas_produto ON mv_vitrine_ofertas (produto_id);
