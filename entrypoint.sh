#!/bin/sh
set -eu

# Decrypt and export directly
if [ -f ".env.enc" ]; then
    echo "Decrypting .env.enc into environment..."
    export SOPS_AGE_KEY_FILE=/opt/api-server/age_key
    sops -d --input-type dotenv --output-type dotenv .env.enc > .env
fi

# Run Java server
java -jar api-server.jar
