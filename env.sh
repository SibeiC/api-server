#!/bin/bash

set -euox pipefail

# Define variables
if [ "${INSTALL_DEV:-false}" = "true" ]; then
  export SERVER_HOST=dev.chencraft.com
else
  export SERVER_HOST=api.chencraft.com
fi

# nginx server settings
export SSL_CERT_PATH=/etc/nginx/certs/fullchain.cer
export SSL_CERT_KEY_PATH=/etc/nginx/certs/server.key
export SSL_SETTINGS="ssl_protocols TLSv1.3 TLSv1.2;
    ssl_ciphers ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384:AES256-GCM-SHA384;
    ssl_ecdh_curve secp384r1;
    ssl_prefer_server_ciphers on;
    ssl_session_tickets off;"
export HTTP_REDIRECT="server {
    listen 80;
    server_name ${SERVER_HOST};
    return 301 https://\$host\$request_uri;
}"

export APP_NAME="api-server"
export WORKING_DIR="/opt/${APP_NAME}"
export SSL_CLIENT_CERT_PATH=/etc/ssl/certs/rootCA.crt

APP_USER="$(id -un)"
APP_GROUP="$(id -gn)"
export APP_USER
export APP_GROUP

# To make envsubst & nginx happy
export proxy_add_x_forwarded_for=\$proxy_add_x_forwarded_for
export scheme=\$scheme
export server_port=\$server_port
export ssl_client_cert=\$ssl_client_cert
export ssl_client_verify=\$ssl_client_verify
export ssl_client_s_dn=\$ssl_client_s_dn
export host=\$host