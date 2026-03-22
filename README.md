# GitHub Access Reporter

A Spring Boot service that connects to GitHub and generates a report showing which users have access to which repositories within an organization.

## Tech Stack
- Java 21
- Spring Boot 4.0.4
- Spring WebFlux (reactive, async API calls)
- Maven

## How to Run

### Prerequisites
- Java 21+
- Maven 3.8+
- A GitHub Personal Access Token

### Setup

1. Clone the repository:
   git clone https://github.com/aniketagrawal190/github-access-reporter.git
   cd github-access-reporter

2. Create the config file:
   Create src/main/resources/application.properties with this content:

   server.port=8080
   github.token=YOUR_GITHUB_TOKEN_HERE
   github.org=YOUR_ORG_NAME_HERE
   github.api.base-url=https://api.github.com
   spring.codec.max-in-memory-size=10MB

3. Run the app:
   ./mvnw spring-boot:run

## Authentication
This service uses a GitHub Personal Access Token. Generate one at:
GitHub → Settings → Developer Settings → Personal Access Tokens → Tokens (classic)

Required scopes:
- repo (full control)
- admin:org (full control)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/v1/health | Health check |
| GET | /api/v1/access-report | Full report - all users and their repos |
| GET | /api/v1/access-report/{username} | Report for a specific user |

## Example Response

{
  "organization": "my-org",
  "totalUsers": 1,
  "users": [
    {
      "username": "aniketagrawal190",
      "avatarUrl": "https://avatars.githubusercontent.com/u/123",
      "totalRepos": 3,
      "repos": [
        {
          "repoName": "backend-api",
          "repoFullName": "my-org/backend-api",
          "permission": "admin",
          "isPrivate": false
        }
      ]
    }
  ]
}

## Design Decisions

- **Reactive WebClient** is used instead of RestTemplate for parallel API calls
- **Pagination** is handled automatically via GitHub's Link header
- **ConcurrentHashMap** is used for thread-safe aggregation
- Errors per repo are logged and skipped so one failure doesn't break the report
- Token is stored in application.properties which is excluded from git via .gitignore
