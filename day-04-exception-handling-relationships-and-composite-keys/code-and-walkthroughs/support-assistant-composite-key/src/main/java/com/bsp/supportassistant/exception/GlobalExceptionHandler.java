package com.bsp.supportassistant.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

// One place that turns exceptions from every controller into an ApiError.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TicketNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AlreadyWatchingException.class)
    public ResponseEntity<ApiError> handleAlreadyWatching(AlreadyWatchingException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // A value in the URL has the wrong type: an id that is not a UUID, or an unknown status.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "'" + ex.getValue() + "' is not a valid value for '" + ex.getName() + "'";
        return build(HttpStatus.BAD_REQUEST, message);
    }

    // A required ?name=value in the URL is missing.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, "The parameter '" + ex.getParameterName() + "' is required");
    }

    // The request body is missing, is not JSON, or holds a value of the wrong type.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "The request body is missing or is not valid JSON");
    }

    // The database refused the data: a required column was empty, or a foreign key blocks a delete.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, "The request breaks a database rule. For example, a required value is missing, or other records still refer to this one");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), message, LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}
