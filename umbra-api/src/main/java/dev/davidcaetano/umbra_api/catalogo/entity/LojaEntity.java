package dev.davidcaetano.umbra_api.catalogo.entity;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "loja")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LojaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CodigoLoja codigo;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String urlBase;

    private String paramAfiliado;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean ativa = true;

    @Column(nullable = false)
    private OffsetDateTime criadoEm;
}
