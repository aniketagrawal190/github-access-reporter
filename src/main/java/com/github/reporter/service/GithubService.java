package com.github.reporter.service;

import com.github.reporter.dto.RepoAccess;
import com.github.reporter.dto.UserAccessReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class GithubService {

    private static final Logger log = LoggerFactory.getLogger(GithubService.class);

    private final WebClient githubWebClient;

    @Value("${github.org}")
    private String org;

    public GithubService(WebClient githubWebClient) {
        this.githubWebClient = githubWebClient;
    }

    public Flux<Map<String, Object>> fetchAllRepos() {
        return fetchPagedResults("/orgs/" + org + "/repos?per_page=100&type=all");
    }

    public Flux<Map<String, Object>> fetchCollaboratorsForRepo(String repoName) {
        String url = "/repos/" + org + "/" + repoName + "/collaborators?per_page=100&affiliation=all";
        return fetchPagedResults(url)
                .onErrorResume(e -> {
                    log.warn("Could not fetch collaborators for repo {}: {}", repoName, e.getMessage());
                    return Flux.empty();
                });
    }

    public Mono<List<UserAccessReport>> generateAccessReport() {

        Map<String, List<RepoAccess>> userRepoMap = new ConcurrentHashMap<>();
        Map<String, String> userAvatarMap = new ConcurrentHashMap<>();

        return fetchAllRepos()
                .flatMap(repo -> {
                    String repoName = (String) repo.get("name");
                    String repoFullName = (String) repo.get("full_name");
                    boolean isPrivate = Boolean.TRUE.equals(repo.get("private"));

                    log.info("Processing repo: {}", repoFullName);

                    return fetchCollaboratorsForRepo(repoName)
                            .map(collaborator -> {
                                String username = (String) collaborator.get("login");
                                String avatarUrl = (String) collaborator.get("avatar_url");
                                String permission = extractPermission(collaborator);

                                userAvatarMap.putIfAbsent(username, avatarUrl);

                                RepoAccess repoAccess = RepoAccess.builder()
                                        .repoName(repoName)
                                        .repoFullName(repoFullName)
                                        .permission(permission)
                                        .isPrivate(isPrivate)
                                        .build();

                                userRepoMap
                                        .computeIfAbsent(username, k -> Collections.synchronizedList(new ArrayList<>()))
                                        .add(repoAccess);

                                return username;
                            });
                })
                .then(Mono.fromCallable(() -> {
                    return userRepoMap.entrySet().stream()
                            .map(entry -> {
                                String username = entry.getKey();
                                List<RepoAccess> repos = entry.getValue();
                                return UserAccessReport.builder()
                                        .username(username)
                                        .avatarUrl(userAvatarMap.getOrDefault(username, ""))
                                        .totalRepos(repos.size())
                                        .repos(repos)
                                        .build();
                            })
                            .sorted(Comparator.comparing(UserAccessReport::getUsername))
                            .collect(Collectors.toList());
                }));
    }

    private Flux<Map<String, Object>> fetchPagedResults(String initialUrl) {
        return Flux.create(sink -> fetchPage(initialUrl, sink));
    }

    @SuppressWarnings("unchecked")
    private void fetchPage(String url, reactor.core.publisher.FluxSink<Map<String, Object>> sink) {
        githubWebClient.get()
                .uri(url)
                .retrieve()
                .toEntityList(Map.class)
                .subscribe(
                        response -> {
                            List<Map<String, Object>> items = (List<Map<String, Object>>) (List<?>) response.getBody();
                            if (items != null) {
                                items.forEach(sink::next);
                            }
                            String linkHeader = response.getHeaders().getFirst("Link");
                            String nextUrl = extractNextUrl(linkHeader);
                            if (nextUrl != null) {
                                fetchPage(nextUrl, sink);
                            } else {
                                sink.complete();
                            }
                        },
                        sink::error
                );
    }

    private String extractNextUrl(String linkHeader) {
        if (linkHeader == null || linkHeader.isBlank()) return null;
        for (String part : linkHeader.split(",")) {
            if (part.contains("rel=\"next\"")) {
                int start = part.indexOf('<') + 1;
                int end = part.indexOf('>');
                if (start > 0 && end > start) {
                    return part.substring(start, end).trim();
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractPermission(Map<String, Object> collaborator) {
        Object permsObj = collaborator.get("permissions");
        if (permsObj instanceof Map) {
            Map<String, Boolean> permissions = (Map<String, Boolean>) permsObj;
            if (Boolean.TRUE.equals(permissions.get("admin"))) return "admin";
            if (Boolean.TRUE.equals(permissions.get("maintain"))) return "maintain";
            if (Boolean.TRUE.equals(permissions.get("push"))) return "push";
            if (Boolean.TRUE.equals(permissions.get("triage"))) return "triage";
            if (Boolean.TRUE.equals(permissions.get("pull"))) return "pull";
        }
        return "unknown";
    }
}