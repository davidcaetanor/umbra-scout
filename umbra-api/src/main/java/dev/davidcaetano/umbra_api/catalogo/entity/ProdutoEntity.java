package dev.davidcaetano.umbra_api.catalogo.entity;

import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Entity
@Table(name = "produto")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProdutoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoProduto tipo;

    @Column(nullable = false)
    private String nome;

    private String categoria;

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
                               String imagemUrl,
                               String chaveItad) {
    }

    public static ProdutoEntity novo(TipoProduto tipo, DadosProduto dados, OffsetDateTime dataHoraAgora) {

        Objects.requireNonNull(dados, "dados é obrigatório");
        Objects.requireNonNull(tipo, "tipo do Produto é obrigatório");
        Objects.requireNonNull(dados.nome(), "nome do Produto é obrigatório");

        String nomeLimpo = strValidoSemEspacosLaterais(dados.nome())
                .orElseThrow(() -> new IllegalArgumentException("nome não pode ser vazio"));

        ProdutoEntity produto = new ProdutoEntity();

        produto.tipo = tipo;
        produto.nome = nomeLimpo;
        produto.categoria = dados.categoria();
        produto.imagemUrl = dados.imagemUrl();
        produto.chaveItad = dados.chaveItad();
        produto.criadoEm = dataHoraAgora;
        produto.atualizadoEm = dataHoraAgora;

        return produto;
    }

    private static Optional<String> strValidoSemEspacosLaterais(String valor) {
        return Optional.ofNullable(valor)
                .map(String::trim)
                .filter(v -> !v.isEmpty());
    }

    private static boolean estaEmDefinir(String valorAtual) {
        return strValidoSemEspacosLaterais(valorAtual).isEmpty();
    }

    public void completarDadosAusentes(DadosProduto dados, OffsetDateTime dataHoraAgora) {

        Objects.requireNonNull(dados, "dados é obrigatório");

        boolean nomeMudou = completarCampoAusente(nome, dados.nome(), v -> nome = v);
        boolean categoriaMudou = completarCampoAusente(categoria, dados.categoria(), v -> categoria = v);
        boolean imagemUrlMudou = completarCampoAusente(imagemUrl, dados.imagemUrl(), v -> imagemUrl = v);
        boolean chaveItadMudou = completarCampoAusente(chaveItad, dados.chaveItad(), v -> chaveItad = v);

        if (nomeMudou || categoriaMudou || imagemUrlMudou || chaveItadMudou) {
            this.atualizadoEm = dataHoraAgora;
        }
    }

    private static boolean completarCampoAusente(String valorAtual, String valorNovo, Consumer<String> setter) {
        if (!estaEmDefinir(valorAtual)) {
            return false;
        }

        Optional<String> valido = strValidoSemEspacosLaterais(valorNovo);
        valido.ifPresent(setter);

        return valido.isPresent();
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
