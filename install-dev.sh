#!/bin/bash

set -euo pipefail

# Wrapper for DEV mode installation via Ansible
# Usage: ./install-dev.sh [extra-ansible-args]

if ! command -v ansible-playbook >/dev/null 2>&1; then
  echo "Error: ansible-playbook not found. Please install Ansible first." >&2
  exit 1
fi

ansible-galaxy collection install -r requirements.yml

# Determine OS to set become prompt and inventory limit
if [[ "$(uname)" == "Darwin" ]]; then
  ASK_BECOME="--ask-become-pass"
  LIMIT_GROUP="macbook"
else
  ASK_BECOME=""
  LIMIT_GROUP="local"
fi

# Run playbook with appropriate host limit and become prompting on macOS only
ansible-playbook -i hosts.ini -l "${LIMIT_GROUP}" playbook.yml -e install_dev=true ${ASK_BECOME:+$ASK_BECOME} "$@"
