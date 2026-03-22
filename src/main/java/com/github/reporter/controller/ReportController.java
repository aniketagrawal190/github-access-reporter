package com.github.reporter.controller;

import com.github.reporter.service.GithubService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final GithubService githubService;

    @Value("${github.org}")
    private String org;

    public ReportController(GithubService githubService) {
        this.githubService = githubService;
    }

    /**
     * GET /api/v1/access-report
     * Returns a full access report: which users have access to which repos.
     */
    @GetMapping("/access-report")
    public Mono<ResponseEntity<Map<String, Object>>> getAccessReport() {
        log.info("Received request for access report for org: {}", org);

        return githubService.generateAccessReport()
                .map(report -> {
                    Map<String, Object> response = Map.of(
                            "organization", org,
                            "totalUsers", report.size(),
                            "users", report
                    );
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    log.error("Failed to generate report: {}", e.getMessage());
                    return Mono.just(
                            ResponseEntity.internalServerError()
                                    .body(Map.of("error", e.getMessage()))
                    );
                });
    }

    /**
     * GET /api/v1/access-report/{username}
     * Returns access report for a single specific user.
     */
    @GetMapping("/access-report/{username}")
    public Mono<ResponseEntity<Object>> getAccessReportForUser(@PathVariable String username) {
        log.info("Received request for user: {}", username);

        return githubService.generateAccessReport()
                .map(report -> {
                    return report.stream()
                            .filter(u -> u.getUsername().equalsIgnoreCase(username))
                            .findFirst()
                            .<ResponseEntity<Object>>map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
                });
    }

    /**
     * GET /api/v1/health
     * Simple health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}