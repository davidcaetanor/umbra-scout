package dev.davidcaetano.umbra_api.notificacao.email;

public record LinhaDoEmail(String nome, String url, String preco, String loja, String alvo, int outrasLojas,
                           boolean baixouDeNovo) {
}
