package dev.davidcaetano.umbra_api.notificacao.email;

import java.util.Objects;

public record MensagemDeEmail(String assunto, String html, String texto) {

    public MensagemDeEmail {
        Objects.requireNonNull(assunto, "assunto é obrigatório");
        Objects.requireNonNull(html, "html é obrigatório");
        Objects.requireNonNull(texto, "texto é obrigatório");
    }
}
