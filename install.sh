#!/bin/bash

set -euo pipefail

# Wrapper to run Ansible-based install (replaces legacy bash logic)
# Usage: ./install.sh [extra-ansible-args]

# Ensure Ansible collections are installed
if ! command -v ansible-playbook >/dev/null 2>&1; then
  echo "Error: ansible-playbook not found. Please install Ansible first." >&2
  exit 1
fi

ansible-galaxy collection install -r requirements.yml

# Run playbook against local inventory by default; pass through any extra args
ansible-playbook -i hosts.ini playbook.yml "$@"