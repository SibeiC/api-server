#!/bin/bash

set -euox pipefail

# --- Variables (edit these if you like) ---
DEPLOY_USER=githubdeploy
DEPLOY_HOME=/home/$DEPLOY_USER
DEPLOY_SSH_DIR=$DEPLOY_HOME/.ssh
DEPLOY_KEY_FILE=$DEPLOY_SSH_DIR/authorized_keys

# --- 1. Create user ---
if id "$DEPLOY_USER" &>/dev/null; then
  echo "User $DEPLOY_USER already exists."
  exit
else
  sudo adduser --disabled-password --gecos "" $DEPLOY_USER
  echo "User $DEPLOY_USER created."
fi

# --- 2. Create SSH folder and set permissions ---
sudo mkdir -p $DEPLOY_SSH_DIR
sudo chmod 700 $DEPLOY_SSH_DIR
sudo touch $DEPLOY_KEY_FILE
sudo chmod 600 $DEPLOY_KEY_FILE
sudo chown -R $DEPLOY_USER:$DEPLOY_USER $DEPLOY_SSH_DIR
sudo usermod -aG docker $DEPLOY_USER

# --- 3. Add public key (paste GitHub Actions deploy key here) ---
echo "Paste your GitHub Actions public SSH key below:"
read -r GITHUB_KEY
echo "$GITHUB_KEY" | sudo tee -a $DEPLOY_KEY_FILE > /dev/null
sudo chown $DEPLOY_USER:$DEPLOY_USER $DEPLOY_KEY_FILE

# --- 4. Setup sudo permissions (optional) ---
# Option A: Full passwordless sudo
# echo "$DEPLOY_USER ALL=(ALL) NOPASSWD:ALL" | sudo tee /etc/sudoers.d/$DEPLOY_USER

# Option B: Restrict to specific commands (recommended)
#echo "$DEPLOY_USER ALL=(ALL) NOPASSWD:" \
#  | sudo tee /etc/sudoers.d/$DEPLOY_USER

echo "User $DEPLOY_USER is ready for GitHub Actions deployments!"
