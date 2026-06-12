#!/bin/sh
set -eu

# Inject secrets from Doppler (project/config implied by DOPPLER_TOKEN service token).
# --fallback keeps restarts working if api.doppler.com is briefly unreachable: doppler
# itself writes/refreshes this encrypted file on every successful fetch (it need not
# pre-exist) and only reads it when the API is unreachable.
# --watch (BETA) restarts the JVM in place whenever the Doppler config changes, so
# app-only secrets rotate without touching the box. Batch multi-secret rotations into
# one `doppler secrets set K1=… K2=…` call (each change event = one restart), and
# rotate nginx/p12-coupled secrets (proxy secret, keystore password) via an ansible
# re-converge instead — a watch restart alone would leave nginx/p12 out of sync.
exec doppler run --watch --fallback /opt/api-server/doppler-fallback.json -- java -jar api-server.jar
