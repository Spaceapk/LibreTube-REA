#!/usr/bin/env bash
set -euo pipefail
# Usage: sign-apk.sh unsigned.apk key.p12 password-file output.apk
: "${ANDROID_HOME:?Set ANDROID_HOME}"
build_tools="${REA_BUILD_TOOLS:-$ANDROID_HOME/build-tools/36.0.0}"
"$build_tools/zipalign" -f -p 4 "$1" "$4.aligned"
trap 'rm -f "$4.aligned"' EXIT
"$build_tools/apksigner" sign --ks "$2" --ks-key-alias libretube-rea --ks-pass "file:$3" --out "$4" "$4.aligned"
"$build_tools/apksigner" verify --verbose --print-certs "$4"
"$build_tools/zipalign" -c -p 4 "$4"
