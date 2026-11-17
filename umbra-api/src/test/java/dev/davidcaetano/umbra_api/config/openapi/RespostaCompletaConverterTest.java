package dev.davidcaetano.umbra_api.config.openapi;

import dev.davidcaetano.umbra_api.config.openapi.dto.response.ExemploResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RespostaCompletaConverterTest {

    @Test
    void respostaDaApiDeveExigirTodosOsCamposEAceitarNuloSoNosMarcados() {
        Map<String, Schema> modelos = modelosDe(ExemploResponse.class);
        Schema exemplo = modelos.get("ExemploResponse");
        Map<String, Schema> propriedades = exemplo.getProperties();

        assertThat(exemplo.getRequired())
                .containsExactlyInAnyOrder("id", "nome", "apelido", "destaque", "principal", "itens");
        assertThat(propriedades.get("apelido").getTypes()).containsExactlyInAnyOrder("string", "null");
        assertThat(propriedades.get("nome").getTypes()).containsExactly("string");
        assertThat(propriedades.get("destaque").getAnyOf()).hasSize(2);
        assertThat(((Schema) propriedades.get("destaque").getAnyOf().get(1)).getTypes()).containsExactly("null");
        assertThat(propriedades.get("principal").get$ref()).endsWith("ItemResponse");
        assertThat(modelos.get("ItemResponse").getRequired()).containsExactly("codigo");
    }

    @Test
    void tipoForaDosDtosDeRespostaDeveFicarComoOSpringdocGerou() {
        Schema registro = modelosDe(RegistroQualquer.class).get("RegistroQualquer");

        assertThat(registro.getRequired()).isNull();
    }

    @Test
    void semConversorSeguinteNaCadeiaNaoDeveResolverNada() {
        Schema resolvido = new RespostaCompletaConverter()
                .resolve(new AnnotatedType(ExemploResponse.class), null, Collections.<ModelConverter>emptyIterator());

        assertThat(resolvido).isNull();
    }

    private static Map<String, Schema> modelosDe(Class<?> tipo) {
        ModelConverters conversores = new ModelConverters(true);
        conversores.addConverter(new RespostaCompletaConverter());
        return conversores.readAll(new AnnotatedType(tipo));
    }

    record RegistroQualquer(String valor) {
    }
}
