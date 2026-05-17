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

Server provisioning + per-release deploys both run the same Ansible playbook (`playbook.yml`).
Tag pushes trigger the playbook automatically from GitHub Actions; `./install.sh` is the
laptop-side entry point for first-time bootstrap and ad-hoc local runs.

Prerequisites:

- Ansible installed on your machine (and on the target host — see "First-time bootstrap" below)
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

- Optionally creates a deploy user githubdeploy, installs a provided GitHub Actions public key,
  and grants it passwordless sudo so CI can re-run the playbook unattended (all skipped in dev mode)
- Ensures working directory at /opt/api-server with correct owner/permissions
- Templates and enables an Nginx site for api-server (HTTP 80 redirect to HTTPS 443)
- Sets upstream proxy port to 8080 (prod) or 8085 (dev)
- Generates a PKCS#12 keystore at /opt/api-server/server.p12 from existing system cert/key
- Optionally stores an age private key in /opt/api-server/age_key (0600) if provided
- When `-e docker_deploy=true ghcr_user=… ghcr_pat_ro=…` is supplied (CI path), also copies
  docker-compose.yml to /opt/api-server, logs in to GHCR, and runs `docker compose pull && up -d`

Prompts during execution:

- TLS keystore password (used to protect server.p12) — skipped when `-e tls_keystore_password=…` is given
- Optional: AGE private key (single line) to write to /opt/api-server/age_key — skipped when `-e age_private_key=…` is given
- Optional (non-dev): GitHub Actions public SSH key for the githubdeploy user

Variables you can override with -e:

- app_user, app_group: default to the Ansible remote user (used for file ownership)
- ssl_cert_path, ssl_cert_key_path: Nginx certificate paths
- p12_cert_path, p12_key_path: certificate/key used for PKCS#12 export
- server_host: defaults to api.chencraft.com (prod) or dev.chencraft.com (dev)
- proxy_port: defaults to 8080 (prod) or 8085 (dev)
- docker_deploy, ghcr_user, ghcr_pat_ro: CI-only; gate the container deploy task block

CI note: ansible-lint runs in GitHub Actions to validate playbook structure. Ensure requirements.yml is kept in sync
with modules used.

### First-time bootstrap (one-time)

On a fresh host, run this once (SSH in as a user with sudo, e.g. `ubuntu`):

1. `sudo apt-get update && sudo apt-get install -y ansible-core git`
2. `git clone <repo> ~/api-server && cd ~/api-server`
3. `./install.sh` — installs collections, creates `githubdeploy` with passwordless sudo and the
   GitHub Actions public key, templates nginx, generates the PKCS#12 keystore, places `/opt/api-server/age_key`.

After bootstrap, the GitHub Actions workflow handles every release automatically: it SCPs the
playbook payload to `~/api-server-deploy/` on the host and runs `ansible-playbook` as `githubdeploy`.

### Required GitHub Actions secrets

- `SERVER_HOST`, `SERVER_USER` (= `githubdeploy`), `SERVER_SSH_KEY` — SSH transport
- `GHCR_PAT_RO` — read-only PAT for `docker login ghcr.io`
- `AGE_PRIVATE_KEY` — multi-line; lets ansible decrypt `.env.enc` to extract the proxy secret
- `TLS_KEYSTORE_PASSWORD` — protects the generated `server.p12`

### Manual re-deploy (no code change)

Use the **Run workflow** button on the `Java CI with Maven` action in GitHub. With no new tag,
the `docker` job is skipped and `deploy` re-runs the playbook against the current `:latest` image —
useful for nginx config tweaks, cert rotations, or recovering from a bad manual edit on the host.

## Release protocol

### Versioning

Tags follow strict semver `vMAJOR.MINOR.PATCH` (e.g. `v0.4.7`). The trigger pattern in
`.github/workflows/maven.yml` is `v*.*.*`, so a tag without three dotted numbers is ignored.

At build time the workflow strips the leading `v` and runs `mvn versions:set` so the Maven
artifact version matches the tag. The Docker image is then pushed to GHCR under **two** tags:

- `ghcr.io/sibeic/api-server:<version>` — immutable, for rollback / pinning
- `ghcr.io/sibeic/api-server:latest` — moving pointer to the most recent release

`docker-compose.yml` references `:latest`, so `docker compose pull && up -d` always deploys
the newest release. Roll back by editing the compose file on the host to pin a previous
`:<version>` tag and re-running the playbook (see "Manual re-deploy" above).

### Cutting a release

```bash
# 1. Make sure local tags match origin so you don't bump on top of an existing one
git fetch --tags origin --prune --prune-tags

# 2. Confirm the highest existing tag, then pick the next bump
git tag --list 'v*' --sort=-v:refname | head -5

# 3. Tag the merge commit on master (annotated, signed if you sign tags)
git tag -a v0.4.8 -m "Release v0.4.8"
git push origin v0.4.8

# 4. Create the matching GitHub release immediately — every tag MUST have one
gh release create v0.4.8 --generate-notes
```

The tag push triggers `Java CI with Maven`:

| Job       | Runs when                            | What it does                                              |
| --------- | ------------------------------------ | --------------------------------------------------------- |
| `ci-test` | every push / PR / dispatch           | Maven build + tests, uploads JAR artifact on tag          |
| `docker`  | tag push only                        | Builds image from the JAR artifact, pushes to GHCR        |
| `deploy`  | tag push **or** `workflow_dispatch`  | SCPs the ansible payload → SSH → `ansible-playbook` runs |

`deploy` SSHes in as `githubdeploy`, runs the playbook with `-e docker_deploy=true`, which
re-converges nginx + PKCS#12 + sudoers + deploy user, then runs `docker compose pull && up -d`
against `/opt/api-server/docker-compose.yml`. End state: the host is converged to whatever
the freshly-pushed `:latest` image and the playbook on `master` describe.

### Watching the release

```bash
# Watch the run after pushing the tag (replace v0.4.8 with your tag)
gh run watch "$(gh run list --workflow 'Java CI with Maven' --branch v0.4.8 --limit 1 --json databaseId -q '.[0].databaseId')"

# Verify the app is up
curl -sk https://api.chencraft.com/actuator/health
```

### Rollback

A bad release can be reverted three ways, in order of preference:

1. **Cut a forward fix** — `git revert <bad-commit> && git tag v0.4.9 && git push --tags`.
   Cleanest history.
2. **Pin the compose file to a previous tag** on the host:
   `ssh githubdeploy@api.chencraft.com 'sed -i "s|:latest|:0.4.7|" /opt/api-server/docker-compose.yml && cd /opt/api-server && docker compose up -d'`.
   Use as a stop-gap, then cut a forward fix to bring `:latest` back into alignment.
3. **Re-deploy a known-good tag via workflow_dispatch** — only works if `:latest` in GHCR
   still points at the good build (i.e. the broken release never finished pushing).

### Tag hygiene

- **Never** retag (`git tag -f v0.4.8 …`). It corrupts the immutable `:<version>` image
  contract — collaborators and the running host can have different bytes for the same name.
- **Always** create the matching `gh release` (`--generate-notes` is fine) so the tag shows
  up in the Releases UI and dependency scanners can attribute CVEs to versions.
- If you forgot to fetch tags before bumping and collided with a remote tag, delete the
  local tag (`git tag -d v0.4.8`), fetch again, pick the next number, and re-push.
