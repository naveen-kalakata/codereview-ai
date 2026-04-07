package com.naveen.codereviewai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

// @ControllerAdvice — Spring scans all controllers. If ANY controller throws
// an exception, Spring checks this class FIRST before returning an error.
// Without this, the user gets an ugly HTML error page or a raw stack trace.
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Catches RuntimeException and all its subclasses (NullPointer, IllegalArgument, etc.)
    // Also catches Spring AI errors (API failures, timeouts, etc.)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Error processing request: {}", ex.getMessage(), ex);

        // Return a clean JSON error instead of a stack trace
        Map<String, Object> error = Map.of(
                "status", 500,
                "error", "Internal Server Error",
                "message", "Failed to process code review. Please try again.",
                "timestamp", LocalDateTime.now().toString()
        );

        // ResponseEntity lets us control BOTH the body AND the HTTP status code
        // HttpStatus.INTERNAL_SERVER_ERROR = 500
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // Catches bad requests — like sending invalid JSON or missing "code" field
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());

        Map<String, Object> error = Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
