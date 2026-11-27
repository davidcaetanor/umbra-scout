package dev.davidcaetano.umbra_api;

import dev.davidcaetano.umbra_api.catalogo.properties.BuscaProperties;
import dev.davidcaetano.umbra_api.catalogo.properties.CatalogoProperties;
import dev.davidcaetano.umbra_api.catalogo.properties.OfertasProperties;
import dev.davidcaetano.umbra_api.catalogo.properties.QualidadeProperties;
import dev.davidcaetano.umbra_api.coleta.itad.properties.ItadProperties;
import dev.davidcaetano.umbra_api.coleta.kabum.properties.KabumProperties;
import dev.davidcaetano.umbra_api.coleta.properties.AgendaColetaProperties;
import dev.davidcaetano.umbra_api.coleta.properties.AlarmeColetaProperties;
import dev.davidcaetano.umbra_api.coleta.properties.AusenciaColetaProperties;
import dev.davidcaetano.umbra_api.coleta.properties.GravacaoPrecoProperties;
import dev.davidcaetano.umbra_api.coleta.properties.ItadColetaProperties;
import dev.davidcaetano.umbra_api.coleta.steam.properties.SteamProperties;
import dev.davidcaetano.umbra_api.config.UmbraProperties;
import dev.davidcaetano.umbra_api.monitoramento.properties.MonitoramentoProperties;
import dev.davidcaetano.umbra_api.notificacao.properties.NotificacaoProperties;
import dev.davidcaetano.umbra_api.usuario.oauth2.properties.LoginExternoProperties;
import dev.davidcaetano.umbra_api.usuario.viacep.properties.ViaCepProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({UmbraProperties.class, ItadProperties.class, SteamProperties.class, KabumProperties.class,
        AgendaColetaProperties.class, AlarmeColetaProperties.class, GravacaoPrecoProperties.class,
        ItadColetaProperties.class, AusenciaColetaProperties.class, BuscaProperties.class, OfertasProperties.class, CatalogoProperties.class, QualidadeProperties.class,
        ViaCepProperties.class, LoginExternoProperties.class, MonitoramentoProperties.class,
        NotificacaoProperties.class})
public class UmbraApiApplication {

    static void main(String[] args) {
        SpringApplication.run(UmbraApiApplication.class, args);
    }

}
