package com.github.reporter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepoAccess {

    private String repoName;       // e.g. "my-awesome-repo"
    private String repoFullName;   // e.g. "my-org/my-awesome-repo"
    private String permission;     // e.g. "admin", "push", "pull"
    private boolean isPrivate;     // true if repo is private
}