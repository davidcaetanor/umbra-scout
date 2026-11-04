package dev.davidcaetano.umbra_api.catalogo.controller;

import dev.davidcaetano.umbra_api.catalogo.service.PeriodoHistorico;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class PeriodoHistoricoConverter implements Converter<String, PeriodoHistorico> {

    @Override
    public PeriodoHistorico convert(String codigo) {
        return PeriodoHistorico.deCodigo(codigo);
    }
}
