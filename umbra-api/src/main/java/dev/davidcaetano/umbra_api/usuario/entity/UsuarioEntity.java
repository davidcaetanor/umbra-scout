package dev.davidcaetano.umbra_api.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    private String senhaHash;

    private String discordId;

    private String googleId;

    @Column(nullable = false)
    private String nomeExibicao;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 8)
    private String cep;

    private String cidade;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 2)
    private String uf;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean ativo = true;

    @Column(nullable = false)
    private OffsetDateTime criadoEm;

    @Column(nullable = false)
    private OffsetDateTime atualizadoEm;

    public static UsuarioEntity comSenha(String email, String senhaHash, String nomeExibicao,
                                         OffsetDateTime dataHoraAgora) {
        UsuarioEntity usuario = novo(email, nomeExibicao, dataHoraAgora);
        usuario.senhaHash = obrigatorio(senhaHash, "senhaHash");
        return usuario;
    }

    public static UsuarioEntity comDiscord(String email, String discordId, String nomeExibicao,
                                           OffsetDateTime dataHoraAgora) {
        UsuarioEntity usuario = novo(email, nomeExibicao, dataHoraAgora);
        usuario.discordId = obrigatorio(discordId, "discordId");
        return usuario;
    }

    public static UsuarioEntity comGoogle(String email, String googleId, String nomeExibicao,
                                          OffsetDateTime dataHoraAgora) {
        UsuarioEntity usuario = novo(email, nomeExibicao, dataHoraAgora);
        usuario.googleId = obrigatorio(googleId, "googleId");
        return usuario;
    }

    public void informarCep(String cep) {
        this.cep = obrigatorio(cep, "cep");
        this.cidade = null;
        this.uf = null;
    }

    public void informarCepComRegiao(String cep, String cidade, String uf) {
        this.cep = obrigatorio(cep, "cep");
        this.cidade = obrigatorio(cidade, "cidade");
        this.uf = obrigatorio(uf, "uf");
    }

    public static String normalizarEmail(String email) {
        return obrigatorio(email, "email").toLowerCase(Locale.ROOT);
    }

    private static UsuarioEntity novo(String email, String nomeExibicao, OffsetDateTime dataHoraAgora) {
        Objects.requireNonNull(dataHoraAgora, "dataHoraAgora é obrigatório");

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.email = normalizarEmail(email);
        usuario.nomeExibicao = obrigatorio(nomeExibicao, "nomeExibicao");
        usuario.criadoEm = dataHoraAgora;
        usuario.atualizadoEm = dataHoraAgora;
        return usuario;
    }

    private static String obrigatorio(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " é obrigatório");
        String limpo = valor.trim();
        if (limpo.isEmpty()) {
            throw new IllegalArgumentException(campo + " não pode ser vazio");
        }
        return limpo;
    }
}
