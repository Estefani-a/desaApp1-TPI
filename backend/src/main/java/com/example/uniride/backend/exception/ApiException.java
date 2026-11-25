package com.example.uniride.backend.exception;

/**
 * Business error thrown by the services. {@link GlobalExceptionHandler} turns it into the
 * HTTP status and body of its {@link ErrorCode}.
 *
 * <pre>{@code throw new ApiException(ErrorCode.NO_SEATS_AVAILABLE);}</pre>
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        this(code, code.defaultMessage());
    }

    public ApiException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
