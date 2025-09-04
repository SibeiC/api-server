#!/usr/bin/sudo bash

set -euox pipefail

source ./env.sh

# Stop the server
systemctl stop nginx

# Remove created directory
rm -rf "${WORKING_DIR}"

# Remove nginx configuration
rm -f "/etc/nginx/sites-enabled/${APP_NAME}.conf"
rm -f "/etc/nginx/sites-available/${APP_NAME}.conf"

# Restart the server
systemctl restart nginx