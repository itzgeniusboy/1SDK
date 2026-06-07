#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NEWBLACKBOX_DIR="$ROOT_DIR/NewBlackbox"
LOADER_DIR="$ROOT_DIR/LOADER"
AAR_OUTPUT_DIR="$NEWBLACKBOX_DIR/Bcore/build/outputs/aar"
LOADER_LIBS_DIR="$LOADER_DIR/app/libs"
LOADER_AAR="$LOADER_LIBS_DIR/Bcore-release.aar"

chmod +x "$NEWBLACKBOX_DIR/gradlew" "$LOADER_DIR/gradlew"

pushd "$NEWBLACKBOX_DIR" >/dev/null
./gradlew --no-daemon :Bcore:assembleRelease
popd >/dev/null

AAR_PATH="$(find "$AAR_OUTPUT_DIR" -maxdepth 1 -name '*release.aar' | sort | head -n 1)"
if [[ -z "$AAR_PATH" ]]; then
  echo "No release AAR found in $AAR_OUTPUT_DIR" >&2
  exit 1
fi

mkdir -p "$LOADER_LIBS_DIR"
cp "$AAR_PATH" "$LOADER_AAR"
echo "Copied $(basename "$AAR_PATH") to $LOADER_AAR"

pushd "$LOADER_DIR" >/dev/null
./gradlew --no-daemon assembleDebug assembleRelease
popd >/dev/null
