package dev.davidcaetano.umbra_api.config.openapi;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Os DTOs de resposta da API sempre serializam todos os campos, inclusive os nulos. Cada componente do
 * record vira campo obrigatório no contrato, e só o marcado com {@link Nullable} aceita {@code null}.
 */
public class RespostaCompletaConverter implements ModelConverter {

    private static final String PACOTE_DA_API = "dev.davidcaetano.umbra_api.";
    private static final String PACOTE_DE_RESPOSTA = ".dto.response";
    private static final String PREFIXO_DE_SCHEMA = "#/components/schemas/";
    private static final String NULO = "null";

    @Override
    public Schema resolve(AnnotatedType tipo, ModelConverterContext contexto, Iterator<ModelConverter> cadeia) {
        if (!cadeia.hasNext()) {
            return null;
        }
        Schema resolvido = cadeia.next().resolve(tipo, contexto, cadeia);
        Class<?> classe = Json.mapper().constructType(tipo.getType()).getRawClass();

        if (resolvido != null && ehRespostaDaApi(classe)) {
            Schema modelo = resolvido.get$ref() == null
                    ? resolvido
                    : contexto.getDefinedModels().get(resolvido.get$ref().substring(PREFIXO_DE_SCHEMA.length()));
            if (modelo != null && modelo.getProperties() != null) {
                completar(modelo, classe.getRecordComponents());
            }
        }
        return resolvido;
    }

    private static boolean ehRespostaDaApi(Class<?> classe) {
        return classe.isRecord()
                && classe.getPackageName().startsWith(PACOTE_DA_API)
                && classe.getPackageName().endsWith(PACOTE_DE_RESPOSTA);
    }

    @SuppressWarnings("unchecked")
    private static void completar(Schema modelo, RecordComponent[] componentes) {
        Map<String, Schema> propriedades = modelo.getProperties();
        modelo.setRequired(Arrays.stream(componentes).map(RecordComponent::getName).toList());

        Arrays.stream(componentes)
                .filter(componente -> componente.getAnnotatedType().isAnnotationPresent(Nullable.class))
                .forEach(componente -> propriedades.computeIfPresent(componente.getName(),
                        (nome, propriedade) -> aceitandoNulo(propriedade)));
    }

    @SuppressWarnings("unchecked")
    private static Schema aceitandoNulo(Schema propriedade) {
        if (propriedade.get$ref() != null) {
            return new Schema<>().anyOf(List.of(new Schema<>().$ref(propriedade.get$ref()),
                    new Schema<>().types(Set.of(NULO))));
        }
        if (propriedade.getTypes() != null) {
            propriedade.addType(NULO);
        }
        return propriedade;
    }
}
