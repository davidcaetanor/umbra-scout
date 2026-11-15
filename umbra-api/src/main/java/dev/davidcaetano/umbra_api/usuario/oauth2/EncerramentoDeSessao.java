package dev.davidcaetano.umbra_api.usuario.oauth2;

import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;

final class EncerramentoDeSessao {

    private static final String COOKIE_DE_SESSAO_PADRAO = "JSESSIONID";

    private EncerramentoDeSessao() {
    }

    static void encerrar(HttpServletRequest request, HttpServletResponse response) {
        HttpSession sessao = request.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        SecurityContextHolder.clearContext();
        response.addCookie(cookieExpirado(request));
    }

    private static Cookie cookieExpirado(HttpServletRequest request) {
        SessionCookieConfig configuracao = request.getServletContext().getSessionCookieConfig();
        String nome = configuracao.getName() != null ? configuracao.getName() : COOKIE_DE_SESSAO_PADRAO;
        String caminho = configuracao.getPath() != null ? configuracao.getPath() : caminhoDoContexto(request);

        Cookie cookie = new Cookie(nome, "");
        cookie.setPath(caminho);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        return cookie;
    }

    private static String caminhoDoContexto(HttpServletRequest request) {
        String contexto = request.getContextPath();
        return contexto == null || contexto.isEmpty() ? "/" : contexto;
    }
}
