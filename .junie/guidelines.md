# Project Development Guidelines

This document captures project-specific knowledge to accelerate onboarding and day-to-day work for advanced contributors. It focuses on conventions, environment, testing, and debugging workflows that apply to this repository.

## 1) Updated README (Project Summary)

Tech stack
- Runtime: Java 21, Spring Boot 3.5.x
- Web: Spring MVC + WebFlux (reactive client)
- Security: Spring Security
- API docs: springdoc-openapi (webmvc-ui)
- JSON: jackson + openapitools/jackson-databind-nullable (JsonNullable support)
- Persistence: Reactive MongoDB (spring-boot-starter-data-mongodb-reactive); tests use embedded Mongo via flapdoodle
- Crypto/PKI: BouncyCastle bcpkix (JDK18+)
- Mail: spring-boot-starter-mail (+ iCloud implementation)
- Cloud integrations: AWS SDK v2 (S3), Cloudflare (custom WebClient), GitHub API (custom)
- Build: Maven; surefire for tests
- Observability: Actuator + Micrometer Prometheus
- Container: Dockerfile + docker-compose

What the service does
- Exposes APIs (some public, some under /secure) for:
  - Certificate issuance/renewal and PEM handling
  - File upload/storage with Cloudflare R2 (S3 compatible)
  - GitHub webhook relay/validation
  - Cloudflare DNS relay
- Configures JSON nullability handling, strict date/time binding, and custom exception handling.

Local setup (developer profile)
- Requirements: Java 21+, Maven 3.9+, Docker (optional), MongoDB (not required when running tests; embedded is used for tests)
- Profiles and props:
  - application.properties: defaults
  - application-dev.properties: developer overrides (activate with SPRING_PROFILES_ACTIVE=dev)
- Env vars (see application.properties and env.sh):
  - Cloudflare, GitHub tokens; S3 endpoint/credentials; iCloud mail creds
- Quick start
  - Build: mvn -P ci clean verify
  - Run: mvn spring-boot:run
  - Run with dev profile: SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
  - Package image: docker build -t api-server:local .

## 2) Testing Information

Running tests
- Fast path (CI profile disables skipTests override):
  - mvn -P ci -DskipTests=false test
- Run a single test by FQN:
  - mvn -P ci -Dtest=com.chencraft.common.service.HashServiceTest test
- Common surefire flags:
  - -Dtest=ClassName     (all methods)
  - -Dtest=Class#method  (single method)

Embedded services in tests
- MongoDB: flapdoodle spring30x is used under test scope. No external Mongo is needed for unit/integration tests.
- Mocking external HTTP: okhttp MockWebServer is available and used in API service tests.

Adding new tests
- Location: src/test/java mirrors src/main/java package structure.
- Use Spring Boot’s @SpringBootTest or slice tests where applicable. For pure units, avoid Spring context to keep tests fast.
- Prefer deterministic clocks (Clock injection via common.config.ClockConfig) and ImmediateTaskExecutor from test utilities when concurrency is involved.
- Test data lives under src/test/resources/public and private for fixtures; prefer adding small, isolated fixtures.

Demonstration test (verified)
- Existing tests already cover units and controllers. Example executions verified during this update:
  - Single test: com.chencraft.common.service.HashServiceTest (passed)
  - Single test: com.chencraft.api.CertificateApiControllerTest (passed)
- To mirror: create a new trivial unit test under src/test/java/com/chencraft/utils if needed, then run with mvn -P ci -Dtest=FQN test.

Troubleshooting test runs
- If no tests execute: ensure -P ci is set (profile enables tests by setting skipTests=false), or pass -DskipTests=false explicitly.
- Java agent warnings from mockito in surefire argLine are benign in local runs.

## 3) Additional Development Information

Code style and conventions
- Java 21, use records where appropriate; Lombok is allowed (provided scope) and annotation processors are configured.
- Public APIs: add Javadoc class-level and for all public methods; describe inputs/outputs, nullability, and error semantics.
- Logging: Prefer SLF4J (via Spring) and structured messages; avoid logging secrets.
- Time: Use java.time with injected Clock where applicable; DateTimeFormatter patterns:
  - LocalDate: yyyy-MM-dd
  - LocalDateTime: yyyy-MM-dd'T'HH:mm:ss.SSS
- JSON nullability: Use org.openapitools.jackson.nullable.JsonNullable for optional fields across API contracts.
- Security:
  - Ensure secure endpoints under com.chencraft.api.secure are authorization-protected; see SecurityConfig.
  - mTLS filter exists; review common.filter.MtlsVerificationFilter for gateway deployments.

Common debugging approaches
- Enable Actuator locally (default) and hit /actuator/health, /actuator/info.
- For WebClient issues, enable wiretap logging via logging.level.org.springframework.web.reactive.function.client=DEBUG (in dev profile).
- Use MockWebServer for reproducing 3rd-party API edge cases in tests.

Project layout highlights
- com.chencraft.configuration: MVC formatters, Swagger, custom validators.
- com.chencraft.common.service.*: hashing, certificate, executor, file (Cloudflare R2), mail.
- com.chencraft.model.*: API models for certificates, DNS, GitHub, persistence documents.
- com.chencraft.api.*: REST interfaces and controllers (secure and public variants).

## 4) Javadoc Coverage Work

During this update, we added or verified class-level Javadocs and public method Javadocs across key files. Guidance when extending:
- Class Javadoc: state responsibility, thread-safety, external IO, and configuration properties used.
- Method Javadoc: describe parameters, return values, exceptions, and non-obvious side-effects.
- Date/time inputs must specify expected format if string-bound (see Application.CustomDateConfig).

Example (from Application):
- Main Application has docs for jsonNullableModule bean and CustomDateConfig describing accepted patterns for LocalDate/LocalDateTime conversion.

When touching APIs, also ensure OpenAPI annotations remain consistent with the Javadocs, but avoid duplicating; keep Javadoc concise and authoritative for developers.

## 5) Commands Cheatsheet
- Build only: mvn -P ci -DskipTests=true clean package
- Verify (with tests): mvn -P ci clean verify
- Run app: mvn spring-boot:run
- Run a single test: mvn -P ci -Dtest=com.chencraft.common.service.HashServiceTest test

Notes
- CI profile is the default for local dev in this repo because parent profile jdk21 sets skipTests=true; make sure to override when running tests.
- Docker image uses the packaged jar via spring-boot-maven-plugin repackage.
