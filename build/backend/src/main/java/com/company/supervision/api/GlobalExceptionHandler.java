package com.company.supervision.api;
import com.company.supervision.entity.dto.ApiResult;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {
 @ExceptionHandler(SecurityException.class) @ResponseStatus(HttpStatus.UNAUTHORIZED) public ApiResult<Void> unauthorized(SecurityException e){return ApiResult.fail(e.getMessage());}
 @ExceptionHandler(ResourceNotFoundException.class) @ResponseStatus(HttpStatus.NOT_FOUND) public ApiResult<Void> notFound(ResourceNotFoundException e){return ApiResult.fail(e.getMessage());}
 @ExceptionHandler({IllegalArgumentException.class,ConstraintViolationException.class,MethodArgumentNotValidException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST) public ApiResult<Void> badRequest(Exception e){return ApiResult.fail(e.getMessage());}
 @ExceptionHandler(IllegalStateException.class) @ResponseStatus(HttpStatus.CONFLICT) public ApiResult<Void> conflict(IllegalStateException e){return ApiResult.fail(e.getMessage());}
}
