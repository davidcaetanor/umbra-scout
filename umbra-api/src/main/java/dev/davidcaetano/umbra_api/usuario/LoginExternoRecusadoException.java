package dev.davidcaetano.umbra_api.usuario;

import dev.davidcaetano.umbra_api.usuario.service.MotivoDeRecusa;

public class LoginExternoRecusadoException extends RuntimeException {

    private final transient MotivoDeRecusa motivo;

    public LoginExternoRecusadoException(MotivoDeRecusa motivo) {
        super("Login externo recusado: " + motivo.codigo());
        this.motivo = motivo;
    }

    public MotivoDeRecusa getMotivo() {
        return motivo;
    }
}
