package dev.davidcaetano.umbra_api.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "qualidade_jogo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QualidadeJogoEntity {

    @Id
    private Long produtoId;

    private Short steamNota;

    private Integer steamAvaliacoes;

    private Short criticaNota;

    private Integer criticaAvaliacoes;

    private Integer listaDesejos;

    private String publicador;

    @Column(nullable = false)
    private OffsetDateTime consultadaEm;
}
