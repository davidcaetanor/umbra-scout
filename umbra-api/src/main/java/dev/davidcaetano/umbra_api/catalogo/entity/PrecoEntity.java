package dev.davidcaetano.umbra_api.catalogo.entity;

import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "preco")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PrecoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private ProdutoEntity produto;

    @Column(nullable = false)
    private long valorCentavos;

    private Long valorOriginalCentavos;

    private Short descontoPct;

    @Column(nullable = false)
    private boolean disponivel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrigemColeta origemColeta;

    @Column(nullable = false)
    private OffsetDateTime coletadoEm;

    private OffsetDateTime expiraEm;

    public static PrecoEntity novo(ProdutoEntity produto,
                                   long valorCentavos,
                                   Long valorOriginalCentavos,
                                   Short descontoPct,
                                   boolean disponivel,
                                   OrigemColeta origemColeta,
                                   OffsetDateTime expiraEm,
                                   OffsetDateTime dataHoraAgora) {

        Objects.requireNonNull(produto, "produto é obrigatório");
        Objects.requireNonNull(origemColeta, "origem coleta é obrigatório");
        Objects.requireNonNull(dataHoraAgora, "dataHoraAgora é obrigatório");

        if (valorCentavos < 0) {
            throw new IllegalArgumentException("valorCentavos não pode ser negativo");
        }

        if (valorOriginalCentavos != null && valorOriginalCentavos < valorCentavos) {
            throw new IllegalArgumentException("valorOriginalCentavos não pode ser menor que valorCentavos");
        }

        if (descontoPct != null && (descontoPct < 0 || descontoPct > 100)) {
            throw new IllegalArgumentException("descontoPct deve estar entre 0 e 100");
        }

        PrecoEntity preco = new PrecoEntity();
        preco.produto = produto;
        preco.valorCentavos = valorCentavos;
        preco.valorOriginalCentavos = valorOriginalCentavos;
        preco.descontoPct = descontoPct;
        preco.disponivel = disponivel;
        preco.origemColeta = origemColeta;
        preco.expiraEm = expiraEm;
        preco.coletadoEm = dataHoraAgora;

        return preco;
    }
}
