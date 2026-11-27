package dev.davidcaetano.umbra_api.notificacao.entity;

import dev.davidcaetano.umbra_api.notificacao.Avisar;
import dev.davidcaetano.umbra_api.notificacao.MotivoDoAviso;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "aviso_de_alvo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvisoDeAlvoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long monitoramentoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MotivoDoAviso motivo;

    @Column(nullable = false, updatable = false)
    private long melhorPrecoCentavos;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime registradoEm;

    @ElementCollection
    @CollectionTable(name = "aviso_de_alvo_loja", joinColumns = @JoinColumn(name = "aviso_id"))
    private List<LojaDoAviso> lojas = new ArrayList<>();

    public static AvisoDeAlvoEntity registrar(Long monitoramentoId, Avisar decisao, OffsetDateTime registradoEm) {
        Objects.requireNonNull(decisao, "decisao é obrigatória");

        AvisoDeAlvoEntity aviso = new AvisoDeAlvoEntity();
        aviso.monitoramentoId = Objects.requireNonNull(monitoramentoId, "monitoramentoId é obrigatório");
        aviso.motivo = decisao.motivo();
        aviso.melhorPrecoCentavos = decisao.melhorPrecoCentavos();
        aviso.registradoEm = Objects.requireNonNull(registradoEm, "registradoEm é obrigatório");
        decisao.cruzaram().forEach(oferta -> aviso.lojas.add(new LojaDoAviso(oferta.ofertaId(), oferta.valorCentavos())));
        return aviso;
    }
}
