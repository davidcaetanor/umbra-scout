package dev.davidcaetano.umbra_api.catalogo.entity;

import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "produto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProdutoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id", nullable = false)
    private LojaEntity loja;

    @Column(nullable = false)
    private String identificadorLoja;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoProduto tipo;

    private String categoria;

    @Column(nullable = false)
    private String url;

    private String imagemUrl;

    private String chaveItad;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean ativo = true;

    @Column(nullable = false)
    private OffsetDateTime criadoEm;

    @Column(nullable = false)
    private OffsetDateTime atualizadoEm;

    public record DadosProduto(String nome,
                               String categoria,
                               String url,
                               String imagemUrl,
                               String chaveItad) {
    }

    public static ProdutoEntity novo(LojaEntity loja,
                                     String identificadorLoja,
                                     TipoProduto tipo,
                                     DadosProduto dados,
                                     OffsetDateTime dataHoraAgora) {

        Objects.requireNonNull(dados, "dados é obrigatório");
        Objects.requireNonNull(loja, "loja é obrigatória");
        Objects.requireNonNull(identificadorLoja, "identificador da loja é obrigatório");
        Objects.requireNonNull(tipo, "tipo do Produto é obrigatório");
        Objects.requireNonNull(dados.nome(), "nome do Produto é obrigatório");
        Objects.requireNonNull(dados.url(), "url é obrigatória");

        ProdutoEntity produto = new ProdutoEntity();

        produto.loja = loja;
        produto.identificadorLoja = identificadorLoja;
        produto.tipo = tipo;
        produto.nome = dados.nome();
        produto.categoria = dados.categoria();
        produto.url = dados.url();
        produto.imagemUrl = dados.imagemUrl();
        produto.chaveItad = dados.chaveItad();
        produto.criadoEm = dataHoraAgora;
        produto.atualizadoEm = dataHoraAgora;

        return produto;
    }

    private static Optional<String> valorValido(String valor) {
        return Optional.ofNullable(valor).filter(v -> !v.isBlank());
    }

    public void atualizarDados(DadosProduto dados, OffsetDateTime dataHoraAgora) {

        Objects.requireNonNull(dados, "dados é obrigatório");

        valorValido(dados.nome()).ifPresent(v -> this.nome = v);
        valorValido(dados.categoria()).ifPresent(v -> this.categoria = v);
        valorValido(dados.url()).ifPresent(v -> this.url = v);
        valorValido(dados.imagemUrl()).ifPresent(v -> this.imagemUrl = v);
        valorValido(dados.chaveItad()).ifPresent(v -> this.chaveItad = v);

        this.atualizadoEm = dataHoraAgora;
    }

    public void desativar(OffsetDateTime dataHoraAgora) {
        this.ativo = false;
        this.atualizadoEm = dataHoraAgora;
    }

    public void reativar(OffsetDateTime dataHoraAgora) {
        this.ativo = true;

        this.atualizadoEm = dataHoraAgora;
    }
}
