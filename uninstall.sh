#!/bin/bash

set -euo pipefail

# Wrapper to run Ansible-based uninstall to mirror install.sh
# Usage: ./uninstall.sh [extra-ansible-args]

if ! command -v ansible-playbook >/dev/null 2>&1; then
  echo "Error: ansible-playbook not found. Please install Ansible first." >&2
  exit 1
fi

ansible-galaxy collection install -r requirements.yml

# Run only the uninstall-tagged tasks
ansible-playbook -i hosts.ini playbook.yml --tags uninstall "$@"