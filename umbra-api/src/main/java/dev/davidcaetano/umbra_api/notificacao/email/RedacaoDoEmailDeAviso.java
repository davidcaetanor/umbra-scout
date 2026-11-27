package dev.davidcaetano.umbra_api.notificacao.email;

import dev.davidcaetano.umbra_api.notificacao.ItemDoEmail;
import dev.davidcaetano.umbra_api.notificacao.LoteDoEmail;
import dev.davidcaetano.umbra_api.notificacao.properties.EnvioDeAvisosProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@ConditionalOnProperty(prefix = "umbra.notificacao", name = "envio-habilitado", havingValue = "true")
public class RedacaoDoEmailDeAviso {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter HORARIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final String MODELO_HTML = "aviso-de-alvo.html";
    private static final String MODELO_TEXTO = "aviso-de-alvo.txt";

    private final ITemplateEngine motor;
    private final String urlDoFront;

    public RedacaoDoEmailDeAviso(ITemplateEngine motorDoEmail, EnvioDeAvisosProperties propriedades) {
        this.motor = motorDoEmail;
        this.urlDoFront = propriedades.urlDoFront();
    }

    public MensagemDeEmail redigir(LoteDoEmail lote, ZonedDateTime conferidoEm) {
        String titulo = titulo(lote.totalDeAlvos());

        Context contexto = new Context(PT_BR);
        contexto.setVariable("titulo", titulo);
        contexto.setVariable("previa", previa(lote));
        contexto.setVariable("itens", lote.itens().stream().map(this::paraLinha).toList());
        contexto.setVariable("restantes", lote.restantes());
        contexto.setVariable("horario", HORARIO.format(conferidoEm));
        contexto.setVariable("urlMonitoramentos", urlDoFront + "/monitoramentos");
        contexto.setVariable("urlMascote", urlDoFront + "/email/umbra-feliz-128.png");

        return new MensagemDeEmail(titulo, motor.process(MODELO_HTML, contexto), motor.process(MODELO_TEXTO, contexto));
    }

    private LinhaDoEmail paraLinha(ItemDoEmail item) {
        return new LinhaDoEmail(item.nome(), urlDoFront + "/catalogo/" + item.produtoId(), reais(item.precoCentavos()),
                item.loja(), reais(item.precoAlvoCentavos()), item.outrasLojas(), item.baixouDeNovo());
    }

    private static String titulo(int alvos) {
        return alvos == 1 ? "1 alvo atingido" : alvos + " alvos atingidos";
    }

    private static String previa(LoteDoEmail lote) {
        ItemDoEmail primeiro = lote.itens().getFirst();
        String previa = primeiro.nome() + " por " + reais(primeiro.precoCentavos()) + " na " + primeiro.loja();
        int outros = lote.totalDeAlvos() - 1;
        return outros == 0 ? previa : previa + ", e mais " + outros;
    }

    private static String reais(long centavos) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(BigDecimal.valueOf(centavos, 2)).replace('\u00a0', ' ');
    }
}
