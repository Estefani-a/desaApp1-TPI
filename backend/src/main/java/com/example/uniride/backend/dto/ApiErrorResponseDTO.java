package com.example.uniride.backend.dto;

import com.example.uniride.backend.exception.ErrorCode;
import java.util.Map;

/**
 * Body of every error response (docs/API.md §2). {@code fields} is only filled for
 * {@link ErrorCode#VALIDATION_ERROR}; otherwise it is serialized as {@code null}.
 */
public record ApiErrorResponseDTO(String code, String message, Map<String, String> fields) {

    public static ApiErrorResponseDTO of(ErrorCode code) {
        return new ApiErrorResponseDTO(code.name(), code.defaultMessage(), null);
    }

    public static ApiErrorResponseDTO of(ErrorCode code, String message) {
        return new ApiErrorResponseDTO(code.name(), message, null);
    }

    public static ApiErrorResponseDTO validation(Map<String, String> fields) {
        return new ApiErrorResponseDTO(ErrorCode.VALIDATION_ERROR.name(), ErrorCode.VALIDATION_ERROR.defaultMessage(), fields);
    }
}
