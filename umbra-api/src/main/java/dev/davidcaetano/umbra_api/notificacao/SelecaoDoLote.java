package dev.davidcaetano.umbra_api.notificacao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class SelecaoDoLote {

    private static final Comparator<ItemDoEmail> MAIOR_QUEDA_PRIMEIRO = SelecaoDoLote::compararQueda;

    private final int itensPorEmail;

    public SelecaoDoLote(int itensPorEmail) {
        if (itensPorEmail <= 0) {
            throw new IllegalArgumentException("itensPorEmail deve ser positivo");
        }
        this.itensPorEmail = itensPorEmail;
    }

    public LoteDoEmail selecionar(List<AlvoEmEnvio> alvos) {
        List<ItemDoEmail> atendidos = new ArrayList<>();
        List<Long> enviados = new ArrayList<>();
        List<Long> descartados = new ArrayList<>();

        for (AlvoEmEnvio alvo : alvos) {
            Optional<ItemDoEmail> item = reconferir(alvo);
            item.ifPresent(atendidos::add);
            (item.isPresent() ? enviados : descartados).addAll(alvo.avisoIds());
        }

        List<ItemDoEmail> ordenados = atendidos.stream()
                .sorted(MAIOR_QUEDA_PRIMEIRO.thenComparing(ItemDoEmail::nome).thenComparingLong(ItemDoEmail::produtoId))
                .toList();
        List<ItemDoEmail> itens = ordenados.subList(0, Math.min(itensPorEmail, ordenados.size()));
        return new LoteDoEmail(itens, ordenados.size() - itens.size(), enviados, descartados);
    }

    private static Optional<ItemDoEmail> reconferir(AlvoEmEnvio alvo) {
        List<LojaEmEnvio> abaixo = alvo.lojas().stream()
                .filter(loja -> loja.atende(alvo.precoAlvoCentavos()))
                .sorted(Comparator.comparingLong(LojaEmEnvio::valorCentavos).thenComparing(LojaEmEnvio::loja))
                .toList();
        if (abaixo.isEmpty()) {
            return Optional.empty();
        }
        LojaEmEnvio melhor = abaixo.getFirst();
        return Optional.of(new ItemDoEmail(alvo.produtoId(), alvo.nome(), melhor.valorCentavos(), melhor.loja(),
                alvo.precoAlvoCentavos(), abaixo.size() - 1, alvo.baixouDeNovo()));
    }

    private static int compararQueda(ItemDoEmail a, ItemDoEmail b) {
        long quedaA = (a.precoAlvoCentavos() - a.precoCentavos()) * b.precoAlvoCentavos();
        long quedaB = (b.precoAlvoCentavos() - b.precoCentavos()) * a.precoAlvoCentavos();
        return Long.compare(quedaB, quedaA);
    }
}
