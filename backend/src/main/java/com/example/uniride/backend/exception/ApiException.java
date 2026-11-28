package com.example.uniride.backend.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Business error thrown by the services. {@link GlobalExceptionHandler} turns it into the
 * HTTP status and body of its {@link ErrorCode}.
 *
 * <pre>{@code throw new ApiException(ErrorCode.NO_SEATS_AVAILABLE);}</pre>
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, String> fields;

    public ApiException(ErrorCode code) {
        this(code, code.defaultMessage());
    }

    public ApiException(ErrorCode code, String message) {
        this(code, message, null);
    }

    private ApiException(ErrorCode code, String message, Map<String, String> fields) {
        super(message);
        this.code = code;
        this.fields = fields;
    }

    /**
     * {@code VALIDATION_ERROR} of a business rule, with the message of each wrong field in
     * {@code fields} (docs/API.md §2), like the errors of {@code @Valid}.
     */
    public static ApiException validation(Map<String, String> fields) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(),
                Collections.unmodifiableMap(new LinkedHashMap<>(fields)));
    }

    public ErrorCode getCode() {
        return code;
    }

    /** Messages by field, or {@code null} if the error is not about specific fields. */
    public Map<String, String> getFields() {
        return fields;
    }
}
