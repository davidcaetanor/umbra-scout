package dev.davidcaetano.umbra_api.coleta.itad;

import dev.davidcaetano.umbra_api.catalogo.service.FonteDeQualidade;
import dev.davidcaetano.umbra_api.catalogo.service.QualidadeDoJogo;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadAvaliacaoResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadEmpresaResponse;
import dev.davidcaetano.umbra_api.coleta.itad.dto.response.ItadJogoInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ItadFonteDeQualidade implements FonteDeQualidade {

    private static final String FONTE_STEAM = "Steam";
    private static final Set<String> FONTES_DA_CRITICA = Set.of("Metascore", "OpenCritic");
    private static final int TAMANHO_DO_PUBLICADOR = 200;

    private final ItadClient itadClient;

    @Override
    public Optional<QualidadeDoJogo> consultar(String chaveItad) {
        return itadClient.buscarInfo(UUID.fromString(chaveItad)).map(ItadFonteDeQualidade::paraQualidade);
    }

    private static QualidadeDoJogo paraQualidade(ItadJogoInfoResponse info) {
        List<ItadAvaliacaoResponse> avaliacoes = info.reviews() == null ? List.of() : info.reviews();
        Optional<ItadAvaliacaoResponse> steam = avaliacoes.stream()
                .filter(avaliacao -> FONTE_STEAM.equals(avaliacao.source()) && avaliacao.score() != null)
                .findFirst();
        Optional<ItadAvaliacaoResponse> critica = avaliacoes.stream()
                .filter(avaliacao -> FONTES_DA_CRITICA.contains(avaliacao.source()) && avaliacao.score() != null)
                .max(Comparator.comparing(ItadAvaliacaoResponse::score));

        return new QualidadeDoJogo(
                steam.map(ItadAvaliacaoResponse::score).orElse(null),
                steam.map(ItadAvaliacaoResponse::count).orElse(null),
                critica.map(ItadAvaliacaoResponse::score).orElse(null),
                critica.map(ItadAvaliacaoResponse::count).orElse(null),
                info.stats() == null ? null : info.stats().waitlisted(),
                publicadorDe(info.publishers()));
    }

    private static String publicadorDe(List<ItadEmpresaResponse> publicadores) {
        if (publicadores == null) {
            return null;
        }
        return publicadores.stream()
                .map(ItadEmpresaResponse::name)
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(nome -> !nome.isEmpty())
                .findFirst()
                .map(nome -> nome.length() > TAMANHO_DO_PUBLICADOR ? nome.substring(0, TAMANHO_DO_PUBLICADOR) : nome)
                .orElse(null);
    }
}
