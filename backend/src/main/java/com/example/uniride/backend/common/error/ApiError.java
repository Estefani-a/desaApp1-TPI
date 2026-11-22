package com.example.uniride.backend.common.error;

import java.util.Map;

/**
 * Body of every error response (docs/API.md §2). {@code fields} is only filled for
 * {@link ErrorCode#VALIDATION_ERROR}; otherwise it is serialized as {@code null}.
 */
public record ApiError(String code, String message, Map<String, String> fields) {

    public static ApiError of(ErrorCode code) {
        return new ApiError(code.name(), code.defaultMessage(), null);
    }

    public static ApiError of(ErrorCode code, String message) {
        return new ApiError(code.name(), message, null);
    }

    public static ApiError validation(Map<String, String> fields) {
        return new ApiError(ErrorCode.VALIDATION_ERROR.name(), ErrorCode.VALIDATION_ERROR.defaultMessage(), fields);
    }
}
