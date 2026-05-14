---
name: api-chencraft
description: Call the api.chencraft.com Spring Boot service. Use this whenever the user asks to hit, curl, probe, upload to, download from, issue/renew/revoke certs against, update DNS via, or otherwise invoke api.chencraft.com or dev.chencraft.com. Loads the client mTLS cert/key path from CHENCRAFT_CLIENT_CERT / CHENCRAFT_CLIENT_KEY (shell rc on macOS/Linux, Credential Manager on Windows) for /secure/** endpoints.
user-invocable: true
allowed-tools:
  - Read
  - Bash(curl *)
  - Bash(echo *)
  - Bash(printenv *)
  - Bash(env)
  - Bash(security find-generic-password *)
  - Bash(powershell *)
---

# /api-chencraft — Invoke api.chencraft.com

This skill drives HTTP calls against the chencraft Spring Boot API. It is the
canonical reference for endpoints, request shapes, and the mTLS handshake.

**Hosts**

| Env  | Base URL                    |
|------|-----------------------------|
| prod | `https://api.chencraft.com` |
| dev  | `https://dev.chencraft.com` |

OpenAPI / Swagger UI is served at `/` and `/v3/api-docs`. When in doubt,
fetch the live spec instead of guessing.

---

## Resolving the client certificate

mTLS is enforced by nginx for `/secure/**`. Public endpoints work without a
client cert. **Never hard-code, prompt for, or paste a cert path.** Always
resolve it from the environment as described below — that is the only
supported source.

### macOS / Linux

Expect two exported variables, set in the user's shell rc file
(`~/.zshrc`, `~/.bashrc`, or `~/.bash_profile` depending on shell):

```sh
export CHENCRAFT_CLIENT_CERT="$HOME/.chencraft/client.crt.pem"
export CHENCRAFT_CLIENT_KEY="$HOME/.chencraft/client.key.pem"
```

Read them with `printenv` / `$VAR`. If either is empty, **stop** and tell the
user to add the exports to their shell rc and re-source it; do not invent a
path. macOS users may alternatively store the values in the login keychain
and export them at shell start — same env-var contract from the skill's
perspective.

### Windows

Expect the same two variables, but resolved from the user environment (set
via `setx` or System Properties → Environment Variables), or from Windows
Credential Manager under generic credentials named `CHENCRAFT_CLIENT_CERT`
and `CHENCRAFT_CLIENT_KEY`. From PowerShell:

```powershell
$cert = $env:CHENCRAFT_CLIENT_CERT
$key  = $env:CHENCRAFT_CLIENT_KEY
# Fallback to Credential Manager:
# $cert = (Get-StoredCredential -Target 'CHENCRAFT_CLIENT_CERT').Password
```

Same rule: if unset, ask the user to populate them. Do not guess.

### Invocation template

```sh
curl --cert "$CHENCRAFT_CLIENT_CERT" \
     --key  "$CHENCRAFT_CLIENT_KEY" \
     https://api.chencraft.com/secure/ping
```

Use `--cacert` only if the user has a private CA in play; the prod cert
chains to a public CA and does not need it.

`curl` accepts a PKCS#12 bundle via `--cert path.p12:password --cert-type P12`
if that is what the user has on disk.

---

## Verifying mTLS works

Before debugging anything else, hit the dedicated probe:

```sh
curl -i --cert "$CHENCRAFT_CLIENT_CERT" --key "$CHENCRAFT_CLIENT_KEY" \
     https://api.chencraft.com/secure/ping
```

- 200 + `pong` → mTLS handshake AND server-side cert lookup both passed
- 401 `mTLS required` → nginx didn't see a valid client cert (check paths)
- 401 `Certificate record not found` → cert presented but unknown to Mongo
- 401 `Certificate revoked` → cert is in the revoked set; issue a new one

The verification rules live in
`src/main/java/com/chencraft/common/filter/MtlsVerificationFilter.java`.
nginx sets `X-Client-Cert` / `X-Client-Verify` on the upstream request; the
filter rejects anything that didn't pass `ssl_verify_client` and (when
`app.mtls.mongo-check-mandatory=true`) cross-checks the SHA-256 fingerprint
against `CertificateRecord` in MongoDB.

---

## Endpoint inventory

Tag column matches the OpenAPI `tags` value. All `/secure/**` paths require
mTLS; everything else is public (but some have their own auth, noted below).

### Public

| Method | Path                       | Tag       | Notes                                                                  |
|--------|----------------------------|-----------|------------------------------------------------------------------------|
| GET    | `/healthcheck`             | healthcheck | Liveness probe; returns `text/plain` `ok`.                          |
| GET    | `/file/{filename}`         | file      | Public R2 download.                                                    |
| GET    | `/file/share?token=…`      | file      | One-time share-link download (UUID token).                             |
| GET    | `/certificate/issue?token=&deviceId=&pemFormat=` | tls | Exchanges an onboarding token for a fresh client cert.       |
| POST   | `/webhook/github/update`   | webhook   | GitHub release webhook. Requires `X-Hub-Signature-256` HMAC-SHA256 header over raw body. Body = GitHub release payload. |

### Secure (mTLS required)

| Method | Path                                        | Tag         | Body / Params                                                |
|--------|---------------------------------------------|-------------|--------------------------------------------------------------|
| GET    | `/secure/ping`                              | tls         | —                                                            |
| GET    | `/secure/authorize`                         | tls         | Returns short-lived `OnboardingToken` for new-device flow.   |
| POST   | `/secure/certificate/renew`                 | tls         | JSON `CertificateRenewal { deviceId?, pemFormat? }`          |
| POST   | `/secure/certificate/revoke`                | tls         | JSON `CertificateRevokeRequest { mongoId?, deviceId?, fingerprintSha256?, revokeReason? }` — at least one identifier required |
| GET    | `/secure/file/{filename}`                   | file        | Private R2 download.                                         |
| POST   | `/secure/file`                              | file        | `multipart/form-data`: `file` (binary) + `destination` (`PUBLIC`/`PRIVATE`/`SHARE`) |
| DELETE | `/secure/file/{filename}?namespace=…`       | file        | `namespace` ∈ `PUBLIC`/`PRIVATE`/`SHARE`                     |
| GET    | `/secure/github/file?repo=&path=&branch=`   | github      | Proxies a private GitHub repo file via server-side token.    |
| PUT    | `/secure/cloudflare/ddns`                   | cloudflare  | JSON `DDNSRequest { hostname, dnsType, myIp?, proxied? }`. If `myIp` omitted, server uses caller's remote address. |
| GET    | `/secure/healthcheck/targets`               | healthcheck | Lists registered probe targets with latest outcome.          |
| POST   | `/secure/healthcheck/targets`               | healthcheck | JSON `HealthCheckTargetRequest { name, url, expectedStatus?, timeoutSeconds?, failureThresholdMinutes?, retryAttempts?, retryDelaySeconds? }` |
| DELETE | `/secure/healthcheck/targets/{id}`          | healthcheck | Soft-deletes a probe target.                                 |

The `/secure/` prefix is wired by
`src/main/java/com/chencraft/common/config/SecurePathConfig.java` — every
controller under package `com.chencraft.api.secure` receives it
automatically. If you add a new endpoint, update this table.

---

## Worked examples

### Issue a new client cert (public, no mTLS)

```sh
curl "https://api.chencraft.com/certificate/issue\
?token=$ONBOARDING_TOKEN&deviceId=$(hostname)&pemFormat=true"
```

### Get an onboarding token from an already-trusted device

```sh
curl --cert "$CHENCRAFT_CLIENT_CERT" --key "$CHENCRAFT_CLIENT_KEY" \
     https://api.chencraft.com/secure/authorize
```

### Upload a private file

```sh
curl --cert "$CHENCRAFT_CLIENT_CERT" --key "$CHENCRAFT_CLIENT_KEY" \
     -F "file=@./report.pdf" \
     -F "destination=PRIVATE" \
     https://api.chencraft.com/secure/file
```

### Update a DDNS record

```sh
curl --cert "$CHENCRAFT_CLIENT_CERT" --key "$CHENCRAFT_CLIENT_KEY" \
     -X PUT https://api.chencraft.com/secure/cloudflare/ddns \
     -H 'Content-Type: application/json' \
     -d '{"hostname":"home.chencraft.com","dnsType":"A","proxied":false}'
```

### Revoke a certificate by fingerprint

```sh
curl --cert "$CHENCRAFT_CLIENT_CERT" --key "$CHENCRAFT_CLIENT_KEY" \
     -X POST https://api.chencraft.com/secure/certificate/revoke \
     -H 'Content-Type: application/json' \
     -d '{"fingerprintSha256":"<64-hex>","revokeReason":"rotated"}'
```

---

## Refresh contract

This skill is the source of truth for the public surface of api.chencraft.com.
Refresh it whenever:

- A controller is added, renamed, or removed under `com.chencraft.api.*`.
- A request/response model changes shape (new required field, renamed field,
  enum value added).
- `SecurePathConfig`, `MtlsVerificationFilter`, or `SecurityConfig` changes
  what counts as protected.
- The set of supported tags in `api/models/TagConstants.java` changes.

The CLAUDE.md rule mandates a pre-commit refresh whenever changes touch any
of those files — keep this doc in lock-step with the code.
