package com.joyhill.demo.common.exception;

import com.joyhill.demo.common.api.BaseResponse;
import com.joyhill.demo.common.api.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<BaseResponse<Void>> handleApi(ApiException e) {
        return ResponseEntity.status(e.getErrorCode().status())
                .body(BaseResponse.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<BaseResponse<Void>> handleValidation(Exception e) {
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(ErrorCode.VALIDATION_ERROR, "입력값을 확인해주세요."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.status())
                .body(BaseResponse.error(ErrorCode.FORBIDDEN, "권한이 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleOther(Exception e) {
        return ResponseEntity.internalServerError()
                .body(BaseResponse.error(ErrorCode.VALIDATION_ERROR, e.getMessage()));
    }
}
