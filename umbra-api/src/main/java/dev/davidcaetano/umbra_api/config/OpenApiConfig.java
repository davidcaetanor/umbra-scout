package dev.davidcaetano.umbra_api.config;

import dev.davidcaetano.umbra_api.comum.openapi.RespostaDeErro;
import dev.davidcaetano.umbra_api.config.openapi.ContratoDaApiCustomizer;
import dev.davidcaetano.umbra_api.config.openapi.RespostaCompletaConverter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_BEARER = "bearer";

    private static final String PROBLEMA = "Problema";
    private static final String TIPO_PROBLEMA = "application/problem+json";

    @Bean
    public OpenAPI umbraOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Umbra API")
                        .version("v1")
                        .description("Catálogo de preços de jogos e hardware e monitoramento de preço-alvo. "
                                + "Erros seguem a RFC 9457 em application/problem+json."))
                .servers(List.of(new Server().url("/")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSchemas(PROBLEMA, problema())
                        .responses(Map.of(
                                RespostaDeErro.PARAMETRO_INVALIDO, erro("Parâmetro, corpo ou campo inválido; "
                                        + "`erros` lista cada campo recusado."),
                                RespostaDeErro.NAO_AUTENTICADO, erro("Token ausente, inválido ou expirado, "
                                        + "ou credencial recusada."),
                                RespostaDeErro.NAO_ENCONTRADO, erro("Recurso inexistente, de outro usuário ou de "
                                        + "conta desativada; as três causas respondem o mesmo corpo."),
                                RespostaDeErro.EM_CONFLITO, erro("O recurso já existe."),
                                RespostaDeErro.REGRA_VIOLADA, erro("Pedido válido recusado por regra de negócio."),
                                RespostaDeErro.SERVICO_INDISPONIVEL, erro("Fonte externa fora ou teto de uso "
                                        + "atingido; tente de novo depois do `Retry-After`.")
                                        .addHeaderObject("Retry-After", new Header()
                                                .description("Segundos até a próxima tentativa.")
                                                .schema(new Schema<Integer>().types(Set.of("integer")))),
                                RespostaDeErro.ERRO_INTERNO, erro("Erro inesperado."))));
    }

    @Bean
    public OpenApiCustomizer contratoDaApi() {
        return new ContratoDaApiCustomizer();
    }

    @Bean
    public RespostaCompletaConverter respostaCompletaConverter() {
        return new RespostaCompletaConverter();
    }

    private static Schema<Object> problema() {
        Schema<Object> erroDeCampo = new Schema<>()
                .types(Set.of("object"))
                .addProperty("campo", texto())
                .addProperty("mensagem", texto())
                .required(List.of("campo", "mensagem"));

        return new Schema<>()
                .types(Set.of("object"))
                .addProperty("type", new Schema<String>().types(Set.of("string")).format("uri"))
                .addProperty("title", texto())
                .addProperty("status", new Schema<Integer>().types(Set.of("integer")).format("int32"))
                .addProperty("detail", texto())
                .addProperty("instance", new Schema<String>().types(Set.of("string")).format("uri-reference"))
                .addProperty("erros", new Schema<>().types(Set.of("array")).items(erroDeCampo)
                        .description("Presente só no 400."))
                .required(List.of("type", "title", "status", "detail", "instance"));
    }

    private static Schema<String> texto() {
        return new Schema<String>().types(Set.of("string"));
    }

    private static ApiResponse erro(String descricao) {
        return new ApiResponse()
                .description(descricao)
                .content(new Content().addMediaType(TIPO_PROBLEMA,
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEMA))));
    }
}
