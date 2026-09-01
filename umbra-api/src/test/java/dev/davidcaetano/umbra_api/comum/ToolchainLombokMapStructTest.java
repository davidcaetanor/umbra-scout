package dev.davidcaetano.umbra_api.comum;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class ToolchainLombokMapStructTest {

    @Test
    void deveGerarMapperQueLeOsGettersDoLombokEEscreveEmRecord() {
        Amostra amostra = new Amostra();
        amostra.setNome("Black Myth: Wukong");
        amostra.setValorCentavos(19990L);

        AmostraResponse response = AmostraMapper.INSTANCIA.paraResponse(amostra);

        assertThat(response.nome()).isEqualTo("Black Myth: Wukong");
        assertThat(response.valorCentavos()).isEqualTo(19990L);
    }
}

@Getter
@Setter
class Amostra {
    private String nome;
    private long valorCentavos;
}

record AmostraResponse(String nome, long valorCentavos) {
}

@Mapper
interface AmostraMapper {
    AmostraMapper INSTANCIA = Mappers.getMapper(AmostraMapper.class);

    AmostraResponse paraResponse(Amostra amostra);
}
