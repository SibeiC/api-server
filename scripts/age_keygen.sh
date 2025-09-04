#!/bin/bash

set -euo pipefail

mkdir -p ~/.config/sops/age

AGE_KEY="${HOME}/.config/sops/age/keys.txt"
if ! [ -f "${AGE_KEY}" ]; then
  age-keygen > "${AGE_KEY}"
  chmod 600 "${AGE_KEY}"
fi

AGE_PUBLIC_KEY=$(age-keygen -y "${AGE_KEY}")
export AGE_PUBLIC_KEY
envsubst < templates/.sops.yaml.template > .sops.yaml