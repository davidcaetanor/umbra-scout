package dev.davidcaetano.umbra_api.monitoramento.entity;

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
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@DynamicUpdate
@Table(name = "monitoramento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonitoramentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long usuarioId;

    @Column(nullable = false, updatable = false)
    private Long produtoId;

    @Column(nullable = false)
    private long precoAlvoCentavos;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean ativo = true;

    @Column(insertable = false, updatable = false)
    private OffsetDateTime ultimoAvisoEm;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(nullable = false)
    private OffsetDateTime atualizadoEm;

    public static MonitoramentoEntity novo(Long usuarioId, Long produtoId, long precoAlvoCentavos,
                                           OffsetDateTime dataHoraAgora) {
        Objects.requireNonNull(dataHoraAgora, "dataHoraAgora é obrigatório");

        MonitoramentoEntity monitoramento = new MonitoramentoEntity();
        monitoramento.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        monitoramento.produtoId = Objects.requireNonNull(produtoId, "produtoId é obrigatório");
        monitoramento.precoAlvoCentavos = alvoPositivo(precoAlvoCentavos);
        monitoramento.criadoEm = dataHoraAgora;
        monitoramento.atualizadoEm = dataHoraAgora;
        return monitoramento;
    }

    public void alterarAlvo(long precoAlvoCentavos, OffsetDateTime dataHoraAgora) {
        Objects.requireNonNull(dataHoraAgora, "dataHoraAgora é obrigatório");
        this.precoAlvoCentavos = alvoPositivo(precoAlvoCentavos);
        this.atualizadoEm = dataHoraAgora;
    }

    public void definirAtivo(boolean ativo, OffsetDateTime dataHoraAgora) {
        this.atualizadoEm = Objects.requireNonNull(dataHoraAgora, "dataHoraAgora é obrigatório");
        this.ativo = ativo;
    }

    private static long alvoPositivo(long precoAlvoCentavos) {
        if (precoAlvoCentavos <= 0) {
            throw new IllegalArgumentException("precoAlvoCentavos deve ser maior que zero");
        }
        return precoAlvoCentavos;
    }
}
