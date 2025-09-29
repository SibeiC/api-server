# api-server

[![Qodana](https://github.com/SibeiC/api-server/actions/workflows/qodana_code_quality.yml/badge.svg)](https://github.com/SibeiC/api-server/actions/workflows/qodana_code_quality.yml)
[![Java CI with Maven](https://github.com/SibeiC/api-server/actions/workflows/maven.yml/badge.svg)](https://github.com/SibeiC/api-server/actions/workflows/maven.yml)
[![GPLv3 License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)

## Overview

Production-ready Spring Boot service exposing:

- Certificate issuance/renewal and PEM handling
- File upload/storage backed by Cloudflare R2 (S3-compatible)
- GitHub webhook relay/validation
- Cloudflare DNS relay

OpenAPI documentation is served via springdoc-openapi (webmvc-ui).

## Tech Stack

- Java 25, Spring Boot 3.5.x (MVC + WebFlux client)
- Security: Spring Security
- JSON: Jackson + JsonNullable (openapitools/jackson-databind-nullable)
- Persistence: Reactive MongoDB (starter-data-mongodb-reactive)
- Crypto/PKI: BouncyCastle bcpkix (JDK18+)
- Mail: spring-boot-starter-mail (iCloud provider)
- Cloud: AWS SDK v2 (S3), Cloudflare (custom WebClient), GitHub API (custom)
- Observability: Actuator + Micrometer Prometheus
- Build: Maven; Surefire for tests
- Container: Dockerfile + docker-compose

## Getting Started (Local)

Requirements: Java 25+, Maven 3.9+, Docker (optional). MongoDB is NOT required for tests (embedded Mongo is used under
test scope).

Environment:

- Base config: src/main/resources/application.properties
- Dev overrides: src/main/resources/application-dev.properties (activate with SPRING_PROFILES_ACTIVE=dev)
- Env vars: see application.properties and env.sh for Cloudflare/GitHub tokens, S3 endpoint/credentials, iCloud mail
  creds

Common tasks:

- Build with tests: mvn -P ci clean verify
- Build only: mvn -P ci -DskipTests=true clean package
- Run app: mvn spring-boot:run
- Run with dev profile: SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
- Docker image: docker build -t api-server:local .

API docs (local): https://dev.chencraft.com/

## Testing

- Run all tests: mvn -P ci -DskipTests=false test
- Run a single test: mvn -P ci -Dtest=com.chencraft.common.service.HashServiceTest test
- Embedded services: Uses flapdoodle embedded Mongo for tests; MockWebServer available for HTTP mocking.

## Project Layout Highlights

- com.chencraft.api.*: REST interfaces and controllers (secure and public variants)
- com.chencraft.common.service.*: hashing, certificate, executor, file (Cloudflare R2), mail
- com.chencraft.configuration: MVC formatters, Swagger, validators
- com.chencraft.model.*: API and persistence models

## Troubleshooting

- If no tests run, ensure profile -P ci is active or pass -DskipTests=false.
- For WebClient debugging, enable DEBUG for org.springframework.web.reactive.function.client in dev profile.
- Actuator endpoints: /actuator/health, /actuator/info
