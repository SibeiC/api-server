#!/bin/bash

set -euo pipefail

# Wrapper for DEV mode installation via Ansible
# Usage: ./install-dev.sh [extra-ansible-args]

if ! command -v ansible-playbook >/dev/null 2>&1; then
  echo "Error: ansible-playbook not found. Please install Ansible first." >&2
  exit 1
fi

ansible-galaxy collection install -r requirements.yml

ansible-playbook -i hosts.ini playbook.yml -e install_dev=true --ask-become-pass "$@"
