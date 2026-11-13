package dev.davidcaetano.umbra_api.usuario.service;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class JwtService {

    private static final JwsHeader CABECALHO = JwsHeader.with(MacAlgorithm.HS256).build();

    private final JwtEncoder jwtEncoder;
    private final String emissor;
    private final Duration validade;
    private final Clock clock;

    public JwtService(JwtEncoder jwtEncoder, String emissor, Duration validade, Clock clock) {
        this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "jwtEncoder é obrigatório");
        this.emissor = Objects.requireNonNull(emissor, "emissor é obrigatório");
        this.validade = Objects.requireNonNull(validade, "validade é obrigatória");
        this.clock = Objects.requireNonNull(clock, "clock é obrigatório");
    }

    public TokenEmitido emitir(Long usuarioId) {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        Instant agora = clock.instant();
        Instant expiraEm = agora.plus(validade);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emissor)
                .subject(usuarioId.toString())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .build();

        String valor = jwtEncoder.encode(JwtEncoderParameters.from(CABECALHO, claims)).getTokenValue();
        return new TokenEmitido(valor, expiraEm);
    }

    public AbstractAuthenticationToken autenticar(Jwt jwt) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(usuarioIdDoAssunto(jwt.getSubject()));
        return UsernamePasswordAuthenticationToken.authenticated(usuario, jwt, List.of());
    }

    private static Long usuarioIdDoAssunto(String assunto) {
        if (assunto == null) {
            throw new InvalidBearerTokenException("Token sem assunto");
        }
        try {
            return Long.valueOf(assunto);
        } catch (NumberFormatException ex) {
            throw new InvalidBearerTokenException("Token com assunto que não identifica usuário", ex);
        }
    }
}
