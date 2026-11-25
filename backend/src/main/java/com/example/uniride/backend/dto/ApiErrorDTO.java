package com.example.uniride.backend.dto;

import com.example.uniride.backend.exception.ErrorCode;
import java.util.Map;

/**
 * Body of every error response (docs/API.md §2). {@code fields} is only filled for
 * {@link ErrorCode#VALIDATION_ERROR}; otherwise it is serialized as {@code null}.
 */
public record ApiErrorDTO(String code, String message, Map<String, String> fields) {

    public static ApiErrorDTO of(ErrorCode code) {
        return new ApiErrorDTO(code.name(), code.defaultMessage(), null);
    }

    public static ApiErrorDTO of(ErrorCode code, String message) {
        return new ApiErrorDTO(code.name(), message, null);
    }

    public static ApiErrorDTO validation(Map<String, String> fields) {
        return new ApiErrorDTO(ErrorCode.VALIDATION_ERROR.name(), ErrorCode.VALIDATION_ERROR.defaultMessage(), fields);
    }
}
