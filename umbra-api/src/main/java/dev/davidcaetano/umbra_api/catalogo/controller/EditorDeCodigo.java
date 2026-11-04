package dev.davidcaetano.umbra_api.catalogo.controller;

import java.beans.PropertyEditorSupport;
import java.util.function.Function;

public class EditorDeCodigo<E extends Enum<E>> extends PropertyEditorSupport {

    private final Function<String, E> deCodigo;

    public EditorDeCodigo(Function<String, E> deCodigo) {
        this.deCodigo = deCodigo;
    }

    @Override
    public void setAsText(String texto) {
        setValue(deCodigo.apply(texto));
    }
}
