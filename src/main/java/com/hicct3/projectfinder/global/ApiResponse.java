package com.hicct3.projectfinder.global;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private final boolean success;
    private final String code;
    private final String message;
    private final T result;

    public static <T> ApiResponse<T> onSuccess(T result)
    {
        return new ApiResponse<>(true, "COMMON_200", "응답 성공했습니다.", result);
    }

    public static <T> ApiResponse<T> onSuccess(String message, T result) {
        return new ApiResponse<>(true, "COMMON_200", message, result);
    }

    public static <T> ApiResponse<T> onError(String message)
    {
        return new ApiResponse<>(false, "COMMON_400", message, null);
    }

    public static <T> ApiResponse<T> onError(ErrorCode errorCode)
    {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResponse<T> onError(ErrorCode errorCode, String message)
    {
        return new ApiResponse<>(false, errorCode.getCode(), message, null);
    }
}