package com.github.reporter.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientError(WebClientResponseException ex) {
        log.error("GitHub API error: {} - {}", ex.getStatusCode(), ex.getMessage());

        String friendlyMessage = switch (ex.getStatusCode().value()) {
            case 401 -> "Invalid or expired GitHub token. Please check your credentials.";
            case 403 -> "Access forbidden. Your token may lack the required scopes (read:org, repo).";
            case 404 -> "Organization or resource not found. Check your org name.";
            case 429 -> "GitHub API rate limit exceeded. Please wait and try again.";
            default  -> "GitHub API error: " + ex.getMessage();
        };

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(Map.of(
                        "error", friendlyMessage,
                        "statusCode", ex.getStatusCode().value()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericError(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred: " + ex.getMessage()));
    }
}