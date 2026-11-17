package dev.davidcaetano.umbra_api.comum;

import java.util.HashSet;
import java.util.Set;

/**
 * Rotas que respondem sem token, no formato "MÉTODO caminho". Fonte única para o teste de isolamento e o do
 * contrato OpenAPI; divergir do {@code SecurityConfig} derruba os dois.
 */
public final class RotasPublicas {

    public static final Set<String> DA_API = Set.of(
            "POST /auth/cadastro",
            "POST /auth/login",
            "POST /auth/oauth2/token",
            "GET /produtos",
            "GET /produtos/{id}",
            "GET /produtos/{id}/historico");

    public static final Set<String> DO_LOGIN_EXTERNO = Set.of(
            "GET /oauth2/authorization/{provedor}",
            "GET /login/oauth2/code/{provedor}");

    public static final Set<String> DE_INFRAESTRUTURA = Set.of(
            "* /error",
            "GET /v3/api-docs",
            "GET /v3/api-docs.yaml",
            "GET /v3/api-docs/swagger-config",
            "GET /swagger-ui.html");

    private RotasPublicas() {
    }

    public static Set<String> mapeadasNoSpring() {
        Set<String> todas = new HashSet<>(DA_API);
        todas.addAll(DE_INFRAESTRUTURA);
        return Set.copyOf(todas);
    }

    public static Set<String> documentadasNoContrato() {
        Set<String> todas = new HashSet<>(DA_API);
        todas.addAll(DO_LOGIN_EXTERNO);
        return Set.copyOf(todas);
    }
}
