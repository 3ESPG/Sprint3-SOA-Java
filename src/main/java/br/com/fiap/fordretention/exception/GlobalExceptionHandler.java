package br.com.fiap.fordretention.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String DADOS_INVALIDOS = "Dados inválidos";

    // ---------- 400 ----------

    /** @Valid em @RequestBody (MethodArgumentNotValidException) e em filtros @ModelAttribute (BindException). */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBind(BindException ex, HttpServletRequest req) {
        List<ApiError.CampoErro> campos = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> campos.add(campoErro(fe)));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> campos.add(new ApiError.CampoErro(ge.getObjectName(), ge.getDefaultMessage())));
        return build(HttpStatus.BAD_REQUEST, DADOS_INVALIDOS, req, campos);
    }

    /** Validação de parâmetros de método (@RequestParam @Min etc.), nativa do Spring MVC 6.1+. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<ApiError.CampoErro> campos = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String nome = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable erro : result.getResolvableErrors()) {
                String campo = erro instanceof FieldError fe ? fe.getField() : nome;
                campos.add(new ApiError.CampoErro(campo, erro.getDefaultMessage()));
            }
        });
        return build(HttpStatus.BAD_REQUEST, DADOS_INVALIDOS, req, campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        List<ApiError.CampoErro> campos = ex.getConstraintViolations().stream()
                .map(v -> {
                    String caminho = v.getPropertyPath().toString();
                    String campo = caminho.contains(".") ? caminho.substring(caminho.lastIndexOf('.') + 1) : caminho;
                    return new ApiError.CampoErro(campo, v.getMessage());
                })
                .toList();
        return build(HttpStatus.BAD_REQUEST, DADOS_INVALIDOS, req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente, JSON malformado ou valor inválido para algum campo", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String mensagem = "Valor '%s' inválido para o parâmetro '%s'".formatted(ex.getValue(), ex.getName());
        return build(HttpStatus.BAD_REQUEST, mensagem, req,
                List.of(new ApiError.CampoErro(ex.getName(), mensagem)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
                                                       HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + ex.getParameterName(), req);
    }

    /** Ordenação por propriedade inexistente, ex.: ?sort=campoQueNaoExiste. */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiError> handlePropertyReference(PropertyReferenceException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Propriedade de ordenação inválida: " + ex.getPropertyName(), req);
    }

    // ---------- 401 / 403 ----------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        String mensagem = ex instanceof DisabledException
                ? "Usuário pendente de aprovação"
                : "Credenciais inválidas";
        return build(HttpStatus.UNAUTHORIZED, mensagem, req);
    }

    /** Lançada por @PreAuthorize (AuthorizationDeniedException) ou pelos services. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.atWarn()
                .addKeyValue("evento", "authz.acesso_negado")
                .addKeyValue("camada", "metodo")
                .log("Acesso negado pelo perfil ou pelo escopo de concessionária");
        String mensagem = ex.getMessage() == null || ex.getMessage().equals("Access Denied")
                ? "Seu perfil não tem permissão para esta operação"
                : ex.getMessage();
        return build(HttpStatus.FORBIDDEN, mensagem, req);
    }

    // ---------- 404 / 405 / 415 ----------

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> handleNotFound(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado: /" + ex.getResourcePath(), req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método %s não suportado".formatted(ex.getMethod()), req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use Content-Type: application/json", req);
    }

    // ---------- 409 / 422 ----------

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ApiError> handleConflict(ConflitoException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    /** Última barreira para unicidade/integridade garantida pelo banco (ex.: corrida entre requisições). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violação de integridade em {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Operação viola uma restrição de integridade dos dados", req);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiError> handleBusinessRule(RegraNegocioException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req);
    }

    // ---------- 500 ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", req);
    }

    private static ApiError.CampoErro campoErro(FieldError fe) {
        if (fe.isBindingFailure()) {
            // Falha de conversão de tipo (ex.: ?status=FOO): evita expor a mensagem técnica do conversor
            return new ApiError.CampoErro(fe.getField(), "valor '%s' inválido".formatted(fe.getRejectedValue()));
        }
        return new ApiError.CampoErro(fe.getField(), fe.getDefaultMessage());
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req) {
        return build(status, message, req, List.of());
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req,
                                                  List<ApiError.CampoErro> campos) {
        return ResponseEntity.status(status).body(ApiError.of(status, message, req.getRequestURI(), campos));
    }
}
