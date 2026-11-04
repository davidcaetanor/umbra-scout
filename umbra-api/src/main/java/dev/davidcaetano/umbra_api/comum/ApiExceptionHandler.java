package dev.davidcaetano.umbra_api.comum;

import dev.davidcaetano.umbra_api.comum.dto.response.ErroDeCampoResponse;
import dev.davidcaetano.umbra_api.comum.excecao.RecursoNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI TIPO_RECURSO_NAO_ENCONTRADO = URI.create("https://umbra.dev/erros/recurso-nao-encontrado");
    private static final URI TIPO_PARAMETRO_INVALIDO = URI.create("https://umbra.dev/erros/parametro-invalido");
    private static final URI TIPO_ERRO_INTERNO = URI.create("https://umbra.dev/erros/erro-interno");

    private static final String TITULO_PARAMETRO_INVALIDO = "Parâmetro inválido";
    private static final String MENSAGEM_VALOR_INVALIDO = "valor inválido";

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problema.setType(TIPO_RECURSO_NAO_ENCONTRADO);
        problema.setTitle("Recurso não encontrado");
        problema.setInstance(URI.create(request.getRequestURI()));
        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail tratarErroInesperado(Exception ex, HttpServletRequest request) {
        String metodoSanitizado = sanitizeForLog(request.getMethod());
        String uriSanitizada = sanitizeForLog(request.getRequestURI());
        log.error("Erro inesperado em {} {}", metodoSanitizado, uriSanitizada, ex);

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
        problema.setType(TIPO_ERRO_INTERNO);
        problema.setTitle("Erro interno");
        problema.setInstance(URI.create(request.getRequestURI()));
        return problema;
    }

    private static String sanitizeForLog(String input) {
        if (input == null) {
            return null;
        }
        return input.replace('\n', '_').replace('\r', '_');
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problema = parametroInvalido("Um ou mais parâmetros da requisição são inválidos.");
        problema.setProperty("erros", ex.getBindingResult().getAllErrors().stream()
                .map(ApiExceptionHandler::erroDeCampo)
                .toList());
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        String parametro = ex instanceof MethodArgumentTypeMismatchException argumento
                ? argumento.getName()
                : ex.getPropertyName();
        String mensagem = mensagemDeTipoInvalido(ex);

        ProblemDetail problema = parametroInvalido("O parâmetro '%s' tem %s.".formatted(parametro, mensagem));
        problema.setProperty("erros", List.of(erro(parametro, mensagem)));
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        if (body instanceof ProblemDetail problema && problema.getInstance() == null
                && request instanceof ServletWebRequest servletRequest) {
            problema.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static ProblemDetail parametroInvalido(String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhe);
        problema.setType(TIPO_PARAMETRO_INVALIDO);
        problema.setTitle(TITULO_PARAMETRO_INVALIDO);
        return problema;
    }

    private static ErroDeCampoResponse erroDeCampo(ObjectError erro) {
        String campo = erro instanceof FieldError erroDeCampo ? erroDeCampo.getField() : erro.getObjectName();

        if (erro.contains(TypeMismatchException.class)) {
            return erro(campo, mensagemDeTipoInvalido(erro.unwrap(TypeMismatchException.class)));
        }
        return erro(campo, erro.getDefaultMessage());
    }

    private static String mensagemDeTipoInvalido(TypeMismatchException ex) {
        Class<?> tipoEsperado = ex.getRequiredType();

        if (tipoEsperado == null || !tipoEsperado.isEnum()) {
            return MENSAGEM_VALOR_INVALIDO;
        }

        String aceitos = Arrays.stream(tipoEsperado.getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        return "%s; valores aceitos: %s".formatted(MENSAGEM_VALOR_INVALIDO, aceitos);
    }

    private static ErroDeCampoResponse erro(String campo, String mensagem) {
        return new ErroDeCampoResponse(campo, Objects.requireNonNullElse(mensagem, MENSAGEM_VALOR_INVALIDO));
    }
}
