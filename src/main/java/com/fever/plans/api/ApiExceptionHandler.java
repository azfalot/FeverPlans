package com.fever.plans.api;

import com.fever.plans.api.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiErrorResponse> badRequest(Exception exception) {
        return errorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> internalServerError() {
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred");
    }

    private ResponseEntity<ApiErrorResponse> errorResponse(
            HttpStatus status, String code, String message) {
        var body = new ApiErrorResponse(new ApiErrorResponse.ApiError(code, message), null);
        return ResponseEntity.status(status).body(body);
    }
}
