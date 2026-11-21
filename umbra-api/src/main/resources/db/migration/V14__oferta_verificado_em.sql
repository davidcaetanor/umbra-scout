ALTER TABLE oferta ADD COLUMN verificado_em TIMESTAMPTZ;

UPDATE oferta o
   SET verificado_em = COALESCE((SELECT max(p.coletado_em) FROM preco p WHERE p.oferta_id = o.id), o.criado_em);

ALTER TABLE oferta ALTER COLUMN verificado_em SET DEFAULT now(),
                   ALTER COLUMN verificado_em SET NOT NULL;
