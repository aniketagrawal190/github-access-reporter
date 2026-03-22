package com.github.reporter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessReport {

    private String username;           // GitHub username
    private String avatarUrl;          // Profile picture URL
    private int totalRepos;            // How many repos they can access
    private List<RepoAccess> repos;    // The actual list of repos + permissions
}