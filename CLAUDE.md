# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

This is a Maven-based Spring Boot 4.0.5 project using Java 25.

```bash
# Build with tests (requires Docker for Testcontainers MongoDB)
mvn -P ci clean verify

# Build without tests
mvn -P ci -DskipTests=true clean package

# Run locally with dev profile
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# Run a single test class
mvn -P ci -Dtest=com.chencraft.common.service.HashServiceTest test

# Run a single test method
mvn -P ci -Dtest=com.chencraft.common.service.HashServiceTest#testValidateHash test
```

**Maven profiles:** `ci` enables tests (default `jdk25` profile skips them). Always use `-P ci` when running tests.

**Tests require Docker** — Testcontainers pulls a MongoDB image automatically.

## Architecture

**Package:** `com.chencraft` — main class is `Application.java`.

### API Layer (`api/`)
- Public endpoints: `*ApiController.java` — certificate issuance, file download, GitHub webhook relay
- Secure endpoints: `api/secure/Secure*ApiController.java` — mTLS-protected (file upload, cert management)
- Controllers implement generated OpenAPI interfaces (`*Api.java`) and delegate to services

### Security (`common/filter/`, `common/config/SecurityConfig.java`)
- **mTLS enforcement** on `/secure/**` paths via `MtlsVerificationFilter` — validates `X-Client-Cert` and `X-Client-Verify` headers set by Nginx reverse proxy
- Certificate fingerprints verified against MongoDB records
- HSTS enabled, CSRF disabled

### Service Layer (`common/service/`)
- `cert/` — PKI operations via BouncyCastle (RSA key gen, X.509 cert issuance, 60-day validity, SHA256withRSA)
- `file/` — File storage on Cloudflare R2 (S3-compatible), token-based file access via `FileTokenService`
- `api/` — GitHub API (webhook signature validation via HMAC-SHA256), Cloudflare DNS queries
- `mail/` — Email alerts via iCloud SMTP
- `executor/` — Async task execution with `ScheduledExecutorService`

### Data Layer (`common/mongo/`, `model/mongo/`)
- **MongoDB (Reactive)** via `ReactiveMongoRepository` interfaces
- `CertificateRecord` — client certificate lifecycle (issuance, fingerprint, revocation via soft-delete)
- `FileToken` — token-based file access records

### Configuration
- `application.properties` — prod config (port 8080, Prometheus metrics on 8957)
- `application-dev.properties` — dev overrides (port 8085, debug logging)
- Environment variables loaded from `.env` file (encrypted as `.env.enc` in Docker, decrypted via sops + age key)

### Test Infrastructure (`src/test/`)
- Mock/local service implementations: `LocalFileService`, `MockMailService`, `MockCertificateService`, `ImmediateTaskExecutor`
- `MongoConfig` sets up Testcontainers MongoDB for integration tests
- `MockWebServer` (OkHttp3) for external API tests
- Controller tests use `WebTestClient` with Spring Security test support

## Deployment

Tag push (`v*.*.*`) triggers `.github/workflows/maven.yml`: build & test → push image to GHCR → SCP the ansible payload (`playbook.yml`, `templates/`, `docker-compose.yml`, `.env.enc`, etc.) to the server → SSH as `githubdeploy` and run `ansible-playbook` unattended. The playbook converges nginx + PKCS#12 + deploy user, then (when `docker_deploy=true` is passed from CI) runs `docker compose pull && up -d` against `/opt/api-server/docker-compose.yml`. Manual `workflow_dispatch` re-runs the deploy job without rebuilding the image (useful for nginx-only changes). Laptop bootstrap is still `./install.sh`.

## Skill maintenance — `api-chencraft`

The `/api-chencraft` skill at `.claude/skills/api-chencraft/SKILL.md` is the canonical client-side reference for hitting `api.chencraft.com`. Before committing, if the staged changes touch any of the following, refresh `SKILL.md` in the same commit so the skill never drifts from the code:

- Anything under `src/main/java/com/chencraft/api/` (controllers or generated `*Api` interfaces — endpoint paths, methods, params, request/response bodies)
- `src/main/java/com/chencraft/common/config/SecurePathConfig.java` (the `/secure/` prefix rule)
- `src/main/java/com/chencraft/common/filter/MtlsVerificationFilter.java` or `common/config/SecurityConfig.java` (what counts as protected, headers consumed)
- `src/main/java/com/chencraft/api/models/TagConstants.java` (OpenAPI tag set)
- Request/response models under `src/main/java/com/chencraft/model/` that are referenced by an API surface

When refreshing, update the endpoint table, examples, and the mTLS section to match. The client-side mTLS combined-PEM path must always be sourced from the `MTLS_PEM` env var (shell rc on macOS/Linux, Windows Credential Manager / user env on Windows) — never hard-code a path in the skill or examples.
