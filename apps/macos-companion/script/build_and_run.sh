#!/bin/zsh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_NAME="Hypixel Legitils"
BUILD_DIR="$ROOT/.build/release"
APP_DIR="$ROOT/dist/$APP_NAME.app"

cd "$ROOT"
swift build -c release

rm -rf "$APP_DIR"
mkdir -p "$APP_DIR/Contents/MacOS"
cp "$BUILD_DIR/HypixelLegitilsCompanion" "$APP_DIR/Contents/MacOS/HypixelLegitilsCompanion"

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
