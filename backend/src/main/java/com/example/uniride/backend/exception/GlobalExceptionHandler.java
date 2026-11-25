package com.example.uniride.backend.exception;

import com.example.uniride.backend.dto.ApiErrorDTO;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single place where exceptions become the error body of docs/API.md §2
 * ({@code { code, message, fields }}). The stack trace is never returned.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** SQLState of PostgreSQL for {@code unique_violation}. */
    private static final String UNIQUE_VIOLATION = "23505";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorDTO> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getCode().status()).body(ApiErrorDTO.of(ex.getCode(), ex.getMessage()));
    }

    /** {@code @Valid} on a request body. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDTO> handleInvalidBody(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> fields.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        return validation(fields);
    }

    /** Constraints on {@code @RequestParam} / {@code @PathVariable} parameters. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorDTO> handleInvalidParameters(HandlerMethodValidationException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors()
                    .forEach(error -> fields.putIfAbsent(name, error.getDefaultMessage()));
        });
        return validation(fields);
    }

    /** {@code @Validated} on a service. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            fields.putIfAbsent(field, violation.getMessage());
        });
        return validation(fields);
    }

    /** For example, a path variable that is not a UUID. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return validation(Map.of(ex.getName(), "Valor inválido."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorDTO.of(ErrorCode.VALIDATION_ERROR, "El cuerpo de la solicitud no es un JSON válido."));
    }

    /**
     * A unique or primary key constraint of the database (for example, two simultaneous
     * inserts with the same UUID or email): the resource already exists. Any other integrity
     * error is a bug and stays a 500. Services translate the cases they know first.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorDTO> handleDataIntegrity(DataIntegrityViolationException ex) {
        if (ex.getMostSpecificCause() instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
            return ResponseEntity.status(ErrorCode.ID_CONFLICT.status()).body(ApiErrorDTO.of(ErrorCode.ID_CONFLICT));
        }
        return handleOther(ex);
    }

    /** Thrown by method security ({@code @PreAuthorize}) inside a controller. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorDTO> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.status()).body(ApiErrorDTO.of(ErrorCode.FORBIDDEN));
    }

    /**
     * Anything else. Spring MVC errors (route not found, method not allowed, missing
     * parameter…) implement {@link ErrorResponse}: they keep their HTTP status and use the
     * closest code of the contract. The rest is a 500 {@code INTERNAL_ERROR}.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleOther(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = errorResponse.getStatusCode();
            ErrorCode code = switch (status.value()) {
                case 401 -> ErrorCode.UNAUTHENTICATED;
                case 403 -> ErrorCode.FORBIDDEN;
                case 404 -> ErrorCode.NOT_FOUND;
                case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
                case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
                default -> ErrorCode.VALIDATION_ERROR;
            };
            return ResponseEntity.status(status).body(ApiErrorDTO.of(code));
        }
        log.error("Error inesperado", ex);
        return ResponseEntity.internalServerError().body(ApiErrorDTO.of(ErrorCode.INTERNAL_ERROR));
    }

    private ResponseEntity<ApiErrorDTO> validation(Map<String, String> fields) {
        return ResponseEntity.badRequest().body(ApiErrorDTO.validation(fields));
    }
}
