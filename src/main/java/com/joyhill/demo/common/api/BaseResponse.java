package com.joyhill.demo.common.api;

public class BaseResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorResponse error;

    private BaseResponse(boolean success, T data, ErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(true, data, null);
    }

    public static BaseResponse<Void> success() {
        return new BaseResponse<>(true, null, null);
    }

    public static BaseResponse<Void> error(ErrorCode code, String message) {
        return new BaseResponse<>(false, null, new ErrorResponse(code.name(), message));
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ErrorResponse getError() {
        return error;
    }
}
