#!/bin/zsh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT_ROOT="$(cd "$ROOT/../.." && pwd)"
APP_NAME="Hypixel Legitils"
BUILD_DIR="$ROOT/.build/release"
APP_DIR="$ROOT/dist/$APP_NAME.app"
LOADER_JAR="$PROJECT_ROOT/loader/build/libs/hypixel-legitils-loader-0.1.0-SNAPSHOT.jar"
MOD_JAR="$PROJECT_ROOT/mod/build/libs/hypixel-legitils-0.1.0-SNAPSHOT.jar"

cd "$ROOT"
swift build -c release

if [[ ! -f "$LOADER_JAR" || ! -f "$MOD_JAR" ]]; then
  echo "Missing bundled Java artifacts. Build :loader:jar and :mod:jar first." >&2
  exit 1
fi
rm -rf "$APP_DIR"
mkdir -p "$APP_DIR/Contents/MacOS"
cp "$BUILD_DIR/HypixelLegitilsCompanion" "$APP_DIR/Contents/MacOS/HypixelLegitilsCompanion"
mkdir -p "$APP_DIR/Contents/Resources/LegitilsRuntime"
cp "$LOADER_JAR" "$APP_DIR/Contents/Resources/LegitilsRuntime/hypixel-legitils-loader-0.1.0-SNAPSHOT.jar"
cp "$MOD_JAR" "$APP_DIR/Contents/Resources/LegitilsRuntime/hypixel-legitils-0.1.0-SNAPSHOT.jar"

cat > "$APP_DIR/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>CFBundleDisplayName</key><string>Hypixel Legitils</string>
  <key>CFBundleExecutable</key><string>HypixelLegitilsCompanion</string>
  <key>CFBundleIdentifier</key><string>com.snkisk.hypixellegitils.companion</string>
  <key>CFBundleName</key><string>Hypixel Legitils</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>LSMinimumSystemVersion</key><string>13.0</string>
</dict></plist>
PLIST

echo "Built: $APP_DIR"
if [[ "${1:-}" == "--launch" ]]; then
  open "$APP_DIR"
fi
