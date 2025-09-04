#!/bin/bash

set -euox pipefail

source ./env.sh

mkdir -p tmp/
envsubst < templates/"${APP_NAME}".conf.template > tmp/"${APP_NAME}".conf
