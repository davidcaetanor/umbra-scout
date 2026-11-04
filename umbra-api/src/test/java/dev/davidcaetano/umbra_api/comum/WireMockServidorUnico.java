package dev.davidcaetano.umbra_api.comum;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public final class WireMockServidorUnico {

    private static final WireMockServer SERVIDOR = iniciar();

    private WireMockServidorUnico() {
    }

    public static WireMockServer servidor() {
        return SERVIDOR;
    }

    private static WireMockServer iniciar() {
        WireMockServer servidor = new WireMockServer(
                WireMockConfiguration.wireMockConfig().dynamicPort().http2PlainDisabled(true));
        servidor.start();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::stop));
        return servidor;
    }
}
