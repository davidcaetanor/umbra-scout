package dev.davidcaetano.umbra_api.usuario.service;

import dev.davidcaetano.umbra_api.usuario.entity.UsuarioEntity;
import dev.davidcaetano.umbra_api.usuario.repository.UsuarioRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum ProvedorDeIdentidade {

    DISCORD("discord", UsuarioRepository::findByDiscordId, UsuarioEntity::getDiscordId,
            UsuarioEntity::vincularDiscord, UsuarioEntity::comDiscord),
    GOOGLE("google", UsuarioRepository::findByGoogleId, UsuarioEntity::getGoogleId,
            UsuarioEntity::vincularGoogle, UsuarioEntity::comGoogle);

    private final String idDeRegistro;
    private final BiFunction<UsuarioRepository, String, Optional<UsuarioEntity>> busca;
    private final Function<UsuarioEntity, String> leitura;
    private final Vinculo vinculo;
    private final Fabrica fabrica;

    ProvedorDeIdentidade(String idDeRegistro, BiFunction<UsuarioRepository, String, Optional<UsuarioEntity>> busca,
                         Function<UsuarioEntity, String> leitura, Vinculo vinculo, Fabrica fabrica) {
        this.idDeRegistro = idDeRegistro;
        this.busca = busca;
        this.leitura = leitura;
        this.vinculo = vinculo;
        this.fabrica = fabrica;
    }

    public String idDeRegistro() {
        return idDeRegistro;
    }

    Optional<UsuarioEntity> buscar(UsuarioRepository repositorio, String idExterno) {
        return busca.apply(repositorio, idExterno);
    }

    boolean jaVinculado(UsuarioEntity usuario) {
        return leitura.apply(usuario) != null;
    }

    void vincular(UsuarioEntity usuario, String idExterno, OffsetDateTime dataHoraAgora) {
        vinculo.vincular(usuario, idExterno, dataHoraAgora);
    }

    UsuarioEntity criar(String email, String idExterno, String nomeExibicao, OffsetDateTime dataHoraAgora) {
        return fabrica.criar(email, idExterno, nomeExibicao, dataHoraAgora);
    }

    @FunctionalInterface
    private interface Vinculo {
        void vincular(UsuarioEntity usuario, String idExterno, OffsetDateTime dataHoraAgora);
    }

    @FunctionalInterface
    private interface Fabrica {
        UsuarioEntity criar(String email, String idExterno, String nomeExibicao, OffsetDateTime dataHoraAgora);
    }
}
