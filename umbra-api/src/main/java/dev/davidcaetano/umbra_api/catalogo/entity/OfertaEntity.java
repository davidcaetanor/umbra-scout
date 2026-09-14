package dev.davidcaetano.umbra_api.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "oferta")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfertaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private ProdutoEntity produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id", nullable = false)
    private LojaEntity loja;

    @Column(nullable = false)
    private String identificadorLoja;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean ativa = true;

    @Column(nullable = false)
    private OffsetDateTime criadoEm;

    @Column(nullable = false)
    private OffsetDateTime atualizadoEm;

    public static OfertaEntity nova(ProdutoEntity produto,
                                    LojaEntity loja,
                                    String identificadorLoja,
                                    String url,
                                    OffsetDateTime dataHoraAgora) {

        Objects.requireNonNull(produto, "produto é obrigatório");
        Objects.requireNonNull(loja, "loja é obrigatória");
        Objects.requireNonNull(identificadorLoja, "identificador da loja é obrigatório");
        Objects.requireNonNull(url, "url é obrigatória");

        validarUrl(url);

        OfertaEntity oferta = new OfertaEntity();

        oferta.produto = produto;
        oferta.loja = loja;
        oferta.identificadorLoja = identificadorLoja;
        oferta.url = url;
        oferta.criadoEm = dataHoraAgora;
        oferta.atualizadoEm = dataHoraAgora;

        return oferta;
    }

    private static void validarUrl(String url) {
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw new IllegalArgumentException("A URL precisa começar com http:// ou https://");
        }
    }

    public void atualizarUrl(String url, OffsetDateTime dataHoraAgora) {
        Optional.ofNullable(url)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .ifPresent(v -> {
                    validarUrl(v);
                    if (!v.equals(this.url)) {
                        this.url = v;
                        this.atualizadoEm = dataHoraAgora;
                    }
                });
    }

    public void desativar(OffsetDateTime dataHoraAgora) {
        this.ativa = false;
        this.atualizadoEm = dataHoraAgora;
    }

    public void reativar(OffsetDateTime dataHoraAgora) {
        this.ativa = true;
        this.atualizadoEm = dataHoraAgora;
    }
}
