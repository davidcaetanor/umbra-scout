package dev.davidcaetano.umbra_api.catalogo.controller;

import dev.davidcaetano.umbra_api.catalogo.service.OrdenacaoCatalogo;
import dev.davidcaetano.umbra_api.catalogo.service.PeriodoHistorico;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice(assignableTypes = ProdutoController.class)
public class CodigosDoCatalogoAdvice {

    @InitBinder
    void registrarEditores(WebDataBinder binder) {
        binder.registerCustomEditor(OrdenacaoCatalogo.class, new EditorDeCodigo<>(OrdenacaoCatalogo::deCodigo));
        binder.registerCustomEditor(PeriodoHistorico.class, new EditorDeCodigo<>(PeriodoHistorico::deCodigo));
    }
}
