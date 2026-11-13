package dev.davidcaetano.umbra_api.usuario.mapper;

import dev.davidcaetano.umbra_api.usuario.dto.response.UsuarioResponse;
import dev.davidcaetano.umbra_api.usuario.entity.UsuarioEntity;
import org.mapstruct.Mapper;

@Mapper
public interface UsuarioMapper {

    UsuarioResponse paraResponse(UsuarioEntity usuario);
}
