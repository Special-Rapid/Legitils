#!/bin/zsh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROJECT_ROOT="$(cd "$ROOT/../.." && pwd)"
APP_NAME="Hypixel Legitils"
BUILD_DIR="$ROOT/.build/release"
APP_DIR="$ROOT/dist/$APP_NAME.app"
LOADER_JAR="$PROJECT_ROOT/loader/build/libs/hypixel-legitils-loader-0.1.0-SNAPSHOT.jar"
MOD_JAR="$PROJECT_ROOT/mod/build/libs/hypixel-legitils-0.1.0-SNAPSHOT.jar"
JAVA_8_HOME="${LEGITILS_JAVA_8_HOME:-}"
if [[ -z "$JAVA_8_HOME" ]]; then
  for candidate in /Library/Java/JavaVirtualMachines/*8*.jdk/Contents/Home(N); do
    if [[ -x "$candidate/bin/java" && -f "$candidate/lib/tools.jar" ]]; then
      JAVA_8_HOME="$candidate"
      break
    fi
  done
fi

cd "$ROOT"
swift build -c release

if [[ ! -x "$JAVA_8_HOME/bin/java" || ! -f "$JAVA_8_HOME/lib/tools.jar" ]]; then
  echo "A full Java 8 JDK is required. Set LEGITILS_JAVA_8_HOME to a JDK 8 home containing lib/tools.jar." >&2
  exit 1
fi
JAVA_HOME="$JAVA_8_HOME" "$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" --no-daemon :loader:jar :mod:jar

EXPECTED_REVISION="$(git -C "$PROJECT_ROOT" describe --always --dirty --abbrev=7)"
ACTUAL_REVISION="$(unzip -p "$MOD_JAR" hypixellegitils-build.properties | sed -n 's/^revision=//p')"
if [[ "$ACTUAL_REVISION" != "$EXPECTED_REVISION" ]]; then
  echo "Bundled MOD revision mismatch: expected $EXPECTED_REVISION, got ${ACTUAL_REVISION:-missing}." >&2
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
