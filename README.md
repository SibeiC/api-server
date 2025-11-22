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

- Java 25, Spring Boot 4 (MVC + WebFlux client)
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

Requirements: Java 25+, Maven 3.9+, Docker (required for Testcontainers). MongoDB is NOT required for tests (Testcontainers starts MongoDB automatically).

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
- Embedded services: Uses Testcontainers MongoDB for tests; MockWebServer available for HTTP mocking (Docker required).

## Project Layout Highlights

- com.chencraft.api.*: REST interfaces and controllers (secure and public variants)
- com.chencraft.common.service.*: hashing, certificate, executor, file (Cloudflare R2), mail
- com.chencraft.configuration: MVC formatters, Swagger, validators
- com.chencraft.model.*: API and persistence models

## Troubleshooting

- If no tests run, ensure profile -P ci is active or pass -DskipTests=false.
- For WebClient debugging, enable DEBUG for org.springframework.web.reactive.function.client in dev profile.
- Actuator endpoints: /actuator/health, /actuator/info

## Provisioning and Nginx setup (Ansible)

The legacy Bash scripts (install.sh and install-dev.sh) have been replaced by an Ansible playbook.

Prerequisites:

- Ansible installed on your machine
- sudo privileges on the target host (for managing nginx and system paths)

Install Ansible Galaxy collections (first time):

- ansible-galaxy collection install -r requirements.yml

Local inventory (default): hosts.ini contains a [local] target using ansible_connection=local.

Run (Production mode):

- ./install.sh

Run (Dev mode):

- ./install-dev.sh

Alternatively, you can invoke Ansible directly and pass extra vars:

- ansible-playbook -i hosts.ini playbook.yml -e install_dev=true

What this playbook does:

- Optionally creates a deploy user githubdeploy and installs a provided GitHub Actions public key (skipped in dev mode)
- Ensures working directory at /opt/api-server with correct owner/permissions
- Templates and enables an Nginx site for api-server (HTTP 80 redirect to HTTPS 443)
- Sets upstream proxy port to 8080 (prod) or 8085 (dev)
- Generates a PKCS#12 keystore at /opt/api-server/server.p12 from existing system cert/key
- Optionally stores an age private key in /opt/api-server/age_key (0600) if provided

Prompts during execution:

- TLS keystore password (used to protect server.p12)
- Optional: AGE private key (single line) to write to /opt/api-server/age_key
- Optional (non-dev): GitHub Actions public SSH key for the githubdeploy user

Variables you can override with -e:

- app_user, app_group: default to the Ansible remote user (used for file ownership)
- ssl_cert_path, ssl_cert_key_path: Nginx certificate paths
- p12_cert_path, p12_key_path: certificate/key used for PKCS#12 export
- server_host: defaults to api.chencraft.com (prod) or dev.chencraft.com (dev)
- proxy_port: defaults to 8080 (prod) or 8085 (dev)

CI note: ansible-lint runs in GitHub Actions to validate playbook structure. Ensure requirements.yml is kept in sync
with modules used.
