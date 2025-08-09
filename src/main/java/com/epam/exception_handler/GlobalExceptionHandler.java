package com.epam.exception_handler;

import com.epam.common.ErrorResponse;
import com.epam.exceptions.ResourceAlreadyExistsException;
import com.epam.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Input validation failed",
                request.getRequestURI()
        );

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errorResponse.addValidationError(fieldName, errorMessage);
        });

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Constraint validation failed",
                request.getRequestURI()
        );

        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errorResponse.addValidationError(fieldName, message);
        });

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            BadRequestException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(
            Exception ex, HttpServletRequest request) {

        String message = ex.getMessage();
        if (ex instanceof MethodArgumentTypeMismatchException) {
            message = "Invalid parameter type for: " +
                    ((MethodArgumentTypeMismatchException) ex).getName();
        } else if (ex instanceof HttpMessageNotReadableException) {
            message = "Invalid request body format";
        }

        return buildErrorResponse(ex, "Bad Request", HttpStatus.BAD_REQUEST, request, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String message = "Database operation failed due to data constraint violation";
        if (ex.getRootCause() != null && ex.getRootCause().getMessage() != null) {
            String rootCause = ex.getRootCause().getMessage();
            if (rootCause.contains("unique constraint") || rootCause.contains("duplicate key")) {
                message = "The record already exists";
            }
        }

        return buildErrorResponse(ex, "Data Integrity Violation", HttpStatus.CONFLICT, request, message);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {

        return buildErrorResponse(ex, "Resource Not Found", HttpStatus.NOT_FOUND, request,
                "The requested URL was not found on the server");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception occurred", ex);

        return buildErrorResponse(ex, "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR, request,
                "An unexpected error occurred. Please try again later or contact support.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String supportedMethods = String.join(", ", Objects.requireNonNull(ex.getSupportedMethods()));
        String message = String.format(
                "Request method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(),
                supportedMethods
        );

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Method Not Allowed",
                message,
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    //Handle Custom Exceptions
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        return buildErrorResponse(ex, "Resource Not Found", HttpStatus.NOT_FOUND, request,
                ex.getMessage());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExistsException(
            ResourceAlreadyExistsException ex, HttpServletRequest request) {

        return buildErrorResponse(ex, "Resource already exists", HttpStatus.CONFLICT, request,
                ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception exception, String error, HttpStatus status, HttpServletRequest request) {
        return buildErrorResponse(exception, error, status, request, exception.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception exception, String error, HttpStatus status, HttpServletRequest request, String message) {

        if (status.is5xxServerError()) {
            log.error("Error response: {} - {}", status, message, exception);
        } else if (status.is4xxClientError()) {
            log.warn("Error response: {} - {}", status, message);
        }

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, status);
    }
}
