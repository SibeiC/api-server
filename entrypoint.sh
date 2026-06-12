#!/bin/sh
set -eu

# Inject secrets from Doppler (project/config implied by DOPPLER_TOKEN service token).
# --fallback keeps restarts working if api.doppler.com is briefly unreachable.
exec doppler run --fallback /opt/api-server/doppler-fallback.json -- java -jar api-server.jar
