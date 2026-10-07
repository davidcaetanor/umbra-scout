package dev.davidcaetano.umbra_api.catalogo.repository;

import dev.davidcaetano.umbra_api.catalogo.entity.PrecoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface PrecoRepository extends JpaRepository<PrecoEntity, Long> {

    @Query(value = """
            SELECT oferta_id AS ofertaId, valor_centavos AS valorCentavos, disponivel, coletado_em AS coletadoEm
              FROM vw_preco_atual
             WHERE oferta_id IN (:ofertaIds)
            """, nativeQuery = true)
    List<UltimoPrecoProjecao> findUltimoPrecoPorOfertaIdIn(@Param("ofertaIds") Collection<Long> ofertaIds);

    @Query(value = """
            SELECT o.id AS ofertaId, l.codigo AS codigoLoja, o.identificador_loja AS identificadorLoja,
                   p.chave_itad AS chaveItad, p.nome AS nomeProduto,
                   v.valor_centavos AS valorCentavos, v.disponivel, v.coletado_em AS coletadoEm
              FROM oferta o
              JOIN loja l ON l.id = o.loja_id
              JOIN produto p ON p.id = o.produto_id
              JOIN vw_preco_atual v ON v.oferta_id = o.id
             WHERE o.ativa
               AND p.ativo
               AND l.codigo IN (:codigosLoja)
            """, nativeQuery = true)
    List<OfertaComUltimoPrecoProjecao> findOfertasAtivasComUltimoPrecoPorCodigoLojaIn(
            @Param("codigosLoja") Collection<String> codigosLoja);

    interface UltimoPrecoProjecao {

        Long getOfertaId();

        long getValorCentavos();

        boolean isDisponivel();

        Instant getColetadoEm();
    }
}
