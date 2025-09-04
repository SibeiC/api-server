#!/bin/bash

set -euox pipefail

source ./env.sh

# Install script for remote server
if ! [ "${INSTALL_DEV:-false}" = "true" ]; then
  ./scripts/create_github_action_user.sh
fi

# Create working directory
sudo mkdir -p "${WORKING_DIR}"
sudo chown "${APP_USER}":"${APP_GROUP}" "${WORKING_DIR}"
sudo chmod 750 "${WORKING_DIR}"

# Move config to the right place
envsubst < templates/"${APP_NAME}".conf.template | sudo tee /etc/nginx/sites-available/"${APP_NAME}".conf > /dev/null
ENABLE_CONF=/etc/nginx/sites-enabled/"${APP_NAME}".conf
if [ ! -e "$ENABLE_CONF" ]; then
    sudo ln -s /etc/nginx/sites-available/"${APP_NAME}".conf "$ENABLE_CONF"
fi

# Install script for local dev
if [ "${INSTALL_DEV:-false}" = "true" ]; then
  sudo sed -i 's/8080/8085/g' /etc/nginx/sites-available/"${APP_NAME}".conf
fi

# Update SSL certificate
SERVER_CERT="/etc/ssl/certs/server.crt"
SERVER_KEY="/etc/ssl/private/server.key"
OUTPUT_P12="${WORKING_DIR}/server.p12"
ALIAS="api-server"
read -s -r -p "Enter TLS keystore password: " PASSWORD

# Generate PKCS12 keystore
echo "Creating PKCS12 keystore at $OUTPUT_P12 ..."
sudo openssl pkcs12 -export \
  -in "$SERVER_CERT" \
  -inkey "$SERVER_KEY" \
  -out "$OUTPUT_P12" \
  -name "$ALIAS" \
  -password pass:"$PASSWORD"

# Set ownership and permissions
echo "Setting ownership to $APP_USER:$APP_GROUP ..."
sudo chown "$APP_USER:$APP_GROUP" "$OUTPUT_P12"
sudo chmod 640 "$OUTPUT_P12"

echo "✅ Keystore created: $OUTPUT_P12"

# Set up AGE private key
AGE_KEY_FILE="${WORKING_DIR}/age_key"
if ! [ -f "${AGE_KEY_FILE}" ]; then
  # Prompt to enter the private key for age
  read -s -r -p "Enter your AGE private key (single line): " PRIVATE_KEY

  # Validate basic format
  if [[ ! "$PRIVATE_KEY" =~ ^AGE-SECRET-KEY- ]]; then
      echo "Error: input does not look like a valid AGE private key."
      exit 1
  fi

  # Save key securely
  echo "$PRIVATE_KEY" > "$AGE_KEY_FILE"
  chmod 600 "$AGE_KEY_FILE"
else
  echo "Age private key already exists, skipping creation."
fi