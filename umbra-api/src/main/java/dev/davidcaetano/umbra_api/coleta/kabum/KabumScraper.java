package dev.davidcaetano.umbra_api.coleta.kabum;

import dev.davidcaetano.umbra_api.catalogo.enums.CodigoLoja;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.catalogo.enums.TipoProduto;
import dev.davidcaetano.umbra_api.coleta.ProdutoColetado;
import dev.davidcaetano.umbra_api.coleta.Scraper;
import dev.davidcaetano.umbra_api.coleta.kabum.dto.response.KabumAtributosResponse;
import dev.davidcaetano.umbra_api.coleta.kabum.dto.response.KabumCatalogoResponse;
import dev.davidcaetano.umbra_api.coleta.kabum.dto.response.KabumProdutoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KabumScraper implements Scraper {

    private static final int PAGE_SIZE = 100;

    private static final Map<String, String> CATEGORIA_POR_PREFIXO_MENU = new LinkedHashMap<>();

    static {
        CATEGORIA_POR_PREFIXO_MENU.put("Hardware/Processadores", "CPU");
        CATEGORIA_POR_PREFIXO_MENU.put("Hardware/Placa de vídeo (VGA)", "GPU");
        CATEGORIA_POR_PREFIXO_MENU.put("Hardware/Placas-mãe", "PLACA_MAE");
        CATEGORIA_POR_PREFIXO_MENU.put("Hardware/Memória RAM", "MEMORIA_RAM");
        CATEGORIA_POR_PREFIXO_MENU.put("Hardware/Fontes", "FONTE");
        CATEGORIA_POR_PREFIXO_MENU.put("Hardware/SSD", "SSD");
        CATEGORIA_POR_PREFIXO_MENU.put("Hardware/Disco Rígido (HD)", "HD");
        CATEGORIA_POR_PREFIXO_MENU.put("Hardware/Coolers", "COOLER");
        CATEGORIA_POR_PREFIXO_MENU.put("Hardware/Kit Hardware", "KIT_HARDWARE");
        CATEGORIA_POR_PREFIXO_MENU.put("Hardware/Placas Interfaces", "PLACA_INTERFACE");
    }

    private final KabumClient kabumClient;

    @Override
    public OrigemColeta fonte() {
        return OrigemColeta.KABUM_API;
    }

    @Override
    public List<ProdutoColetado> coletar() {
        List<KabumProdutoResponse> itens = buscarCatalogoCompleto();

        List<ProdutoColetado> resultado = new ArrayList<>();

        for (KabumProdutoResponse item : itens) {
            String categoria = mapearCategoria(item.attributes().menu());

            if (categoria == null) {
                log.warn("Descartando item Kabum com menu fora do escopo mapeado: id={} menu={}",
                        item.id(), item.attributes().menu());
                continue;
            }

            resultado.add(toProdutoColetado(item, categoria));
        }

        return resultado;
    }

    private List<KabumProdutoResponse> buscarCatalogoCompleto() {
        KabumCatalogoResponse primeiraPagina = kabumClient.buscarPaginaHardware(1, PAGE_SIZE);

        List<KabumProdutoResponse> itens = new ArrayList<>(primeiraPagina.data());

        int totalPaginas = primeiraPagina.meta().totalPagesCount();
        for (int pagina = 2; pagina <= totalPaginas; pagina++) {
            KabumCatalogoResponse proximaPagina = kabumClient.buscarPaginaHardware(pagina, PAGE_SIZE);
            itens.addAll(proximaPagina.data());
        }

        return itens;
    }

    private static String mapearCategoria(String menu) {
        if (menu == null) {
            return null;
        }

        for (Map.Entry<String, String> entrada : CATEGORIA_POR_PREFIXO_MENU.entrySet()) {
            if (menu.startsWith(entrada.getKey())) {
                return entrada.getValue();
            }
        }

        return null;
    }

    private static ProdutoColetado toProdutoColetado(KabumProdutoResponse item, String categoria) {
        KabumAtributosResponse atributos = item.attributes();

        boolean temDesconto = atributos.discountPercentage() != 0;

        boolean disponivel = atributos.available()
                && atributos.stock() > 0
                && atributos.priceWithDiscount().compareTo(BigDecimal.ZERO) > 0;

        return new ProdutoColetado(
                CodigoLoja.KABUM,
                String.valueOf(item.id()),
                TipoProduto.HARDWARE,
                atributos.title(),
                categoria,
                "https://www.kabum.com.br/produto/" + item.id() + "/" + atributos.productLink(),
                null,
                null,
                paraCentavos(atributos.priceWithDiscount()),
                temDesconto ? paraCentavos(atributos.price()) : null,
                temDesconto ? (short) atributos.discountPercentage() : null,
                disponivel,
                OrigemColeta.KABUM_API,
                OffsetDateTime.now(),
                null
        );
    }

    private static long paraCentavos(BigDecimal valor) {
        return valor.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
