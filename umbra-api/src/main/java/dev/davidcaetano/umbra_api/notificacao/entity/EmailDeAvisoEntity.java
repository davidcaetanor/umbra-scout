package dev.davidcaetano.umbra_api.notificacao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "email_de_aviso")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailDeAvisoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime enviadoEm;

    @Column(nullable = false, updatable = false)
    private int itens;

    public static EmailDeAvisoEntity registrar(Long usuarioId, OffsetDateTime enviadoEm, int itens) {
        EmailDeAvisoEntity email = new EmailDeAvisoEntity();
        email.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        email.enviadoEm = Objects.requireNonNull(enviadoEm, "enviadoEm é obrigatório");
        email.itens = itens;
        return email;
    }
}
