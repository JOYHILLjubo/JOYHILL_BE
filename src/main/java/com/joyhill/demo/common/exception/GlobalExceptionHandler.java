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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

    // 파일 업로드(공지 이미지, 프로필 사진) 관련 — 안 잡으면 아래 catch-all이 전부 500으로 덮어씀
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(ErrorCode.VALIDATION_ERROR, "파일이 너무 큽니다. 30MB 이하로 올려주세요."));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingPart(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(ErrorCode.VALIDATION_ERROR, "업로드할 파일이 없습니다."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(ErrorCode.FORBIDDEN.status())
                .body(BaseResponse.error(ErrorCode.FORBIDDEN, "권한이 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleOther(Exception e) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Unhandled exception", e);
        return ResponseEntity.internalServerError()
                .body(BaseResponse.error(ErrorCode.VALIDATION_ERROR, "서버 오류가 발생했습니다."));
    }
}
