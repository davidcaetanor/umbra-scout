package dev.davidcaetano.umbra_api.coleta.service;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.coleta.OfertaColetada;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
final class PreparadorDeRodada {

    private static final int TAMANHO_CHUNK = 500;

    List<List<GrupoDeOfertas>> preparar(List<OfertaColetada> ofertas) {

        List<OfertaColetada> semDuplicatas = removerDuplicatas(ofertas);

        Map<GrupoChave, List<OfertaColetada>> grupos = semDuplicatas.stream()
                .collect(Collectors.groupingBy(GrupoChave::de, LinkedHashMap::new, Collectors.toList()));

        List<GrupoDeOfertas> gruposDeOfertas = grupos.entrySet().stream()
                .map(entrada -> new GrupoDeOfertas(entrada.getKey(), entrada.getValue()))
                .toList();

        return particionarEmChunks(gruposDeOfertas);
    }

    private List<OfertaColetada> removerDuplicatas(List<OfertaColetada> ofertas) {

        Map<ChaveOfertaColetada, OfertaColetada> semDuplicatas = new LinkedHashMap<>();

        for (OfertaColetada oferta : ofertas) {
            ChaveOfertaColetada chave = new ChaveOfertaColetada(oferta.loja(), oferta.identificadorLoja());
            if (semDuplicatas.putIfAbsent(chave, oferta) != null) {
                log.debug("Oferta duplicada nesta rodada descartada: loja={}, identificadorLoja={}",
                        oferta.loja(), oferta.identificadorLoja());
            }
        }

        return new ArrayList<>(semDuplicatas.values());
    }

    private List<List<GrupoDeOfertas>> particionarEmChunks(Collection<GrupoDeOfertas> grupos) {

        List<List<GrupoDeOfertas>> chunks = new ArrayList<>();
        List<GrupoDeOfertas> chunkAtual = new ArrayList<>();
        int ofertasNoChunkAtual = 0;

        for (GrupoDeOfertas grupo : grupos) {
            int tamanhoDoGrupo = grupo.ofertas().size();

            if (!chunkAtual.isEmpty() && ofertasNoChunkAtual + tamanhoDoGrupo > TAMANHO_CHUNK) {
                chunks.add(chunkAtual);
                chunkAtual = new ArrayList<>();
                ofertasNoChunkAtual = 0;
            }

            chunkAtual.add(grupo);
            ofertasNoChunkAtual += tamanhoDoGrupo;
        }

        if (!chunkAtual.isEmpty()) {
            chunks.add(chunkAtual);
        }

        return chunks;
    }

    record GrupoChave(String chaveItad, CodigoLoja loja, String identificadorLoja) {

        static GrupoChave de(OfertaColetada oferta) {
            return oferta.chaveItad() != null
                    ? new GrupoChave(oferta.chaveItad(), null, null)
                    : new GrupoChave(null, oferta.loja(), oferta.identificadorLoja());
        }
    }

    record GrupoDeOfertas(GrupoChave chave, List<OfertaColetada> ofertas) {
    }

    private record ChaveOfertaColetada(CodigoLoja loja, String identificadorLoja) {
    }
}
