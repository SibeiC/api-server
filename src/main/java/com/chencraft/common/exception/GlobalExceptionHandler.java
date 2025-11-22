package com.chencraft.common.exception;

import com.chencraft.api.ApiException;
import com.chencraft.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<@NonNull ErrorResponse> handleApiException(Exception ex, HandlerMethod handlerMethod, HttpServletRequest request) {
        String handlerInfo = handlerMethod != null
                ? handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName()
                : "UnknownHandler";

        log.error("Exception in [{}] while handling [{} {}]: {}",
                  handlerInfo,
                  request.getMethod(),
                  request.getRequestURI(),
                  ex.getMessage(),
                  ex);

        HttpStatus code = ex instanceof ApiException ? ((ApiException) ex).getCode() : HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse error = new ErrorResponse().status(code).message(ex.getMessage());
        return ResponseEntity.status(code).body(error);
    }
}
