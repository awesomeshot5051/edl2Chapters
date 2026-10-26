#!/bin/bash
# Builds the Windows installer (Edl2Chapters-<version>.exe) on Linux.
# Needs: Launch4j, makensis, and a Windows jlink runtime (a folder with bin/java.exe).
# Overrides: LAUNCH4J_JAR, RUNTIME_DIR, JAVA_HOME (a JDK 25 to run Gradle).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LAUNCH4J_JAR="${LAUNCH4J_JAR:-/opt/launch4j/launch4j.jar}"
RUNTIME_DIR="${RUNTIME_DIR:-/mnt/Data/GUI/SetupScript/runtime}"
REQUIRED_MODULES=(java.base java.desktop java.logging java.xml java.net.http jdk.unsupported)

# 1. Version from build.gradle (version = '1.3.0'); 1.3.0 becomes 1.3.0.0 for the Windows version blocks.
VERSION=$(grep -E "^version *= *'" "$ROOT/build.gradle" | head -1 | cut -d"'" -f2)
[ -n "$VERSION" ] || { echo "Could not read the version from build.gradle" >&2; exit 1; }
VERSION_QUAD="${VERSION}.0"

# 2. The Windows runtime has to carry the modules the app needs.
[ -f "$RUNTIME_DIR/bin/java.exe" ] || { echo "No Windows runtime at $RUNTIME_DIR (set RUNTIME_DIR)" >&2; exit 1; }
for module in "${REQUIRED_MODULES[@]}"; do
    grep -q "\b$module\b" "$RUNTIME_DIR/release" || echo "Warning: runtime lacks $module; rebuild it with jlink --add-modules" >&2
done

BUILD="$ROOT/build/windows-exe"
APP="$BUILD/app/Edl2Chapters"
OUT_DIR="$ROOT/build/distributions"
ICON="$ROOT/src/main/resources/icons/ETIcon.ico"
rm -rf "$BUILD"
mkdir -p "$APP" "$OUT_DIR"

# 3. Jar with the Windows JavaFX natives.
echo "Building the jar..."
"$ROOT/gradlew" -p "$ROOT" -q windowsFatJar
JAR="$ROOT/build/libs/edl2Chapters-${VERSION}-win-all.jar"
[ -f "$JAR" ] || { echo "Missing $JAR" >&2; exit 1; }

# 4. Launch4j: the GUI launcher and the console launcher used by the edl2chapters command.
wrap() { # <header> <exe name> <description>
    local config="$BUILD/$2.xml"
    sed -e "s|@HEADER@|$1|" -e "s|@JAR@|$JAR|" -e "s|@OUTFILE@|$APP/$2|" -e "s|@ICON@|$ICON|" \
        -e "s|@VERSION_QUAD@|$VERSION_QUAD|g" -e "s|@DESCRIPTION@|$3|" -e "s|@ORIGINAL@|$2|" \
        "$ROOT/packaging/windows/launch4j.xml" > "$config"
    java -jar "$LAUNCH4J_JAR" "$config"
}
echo "Building the launchers..."
wrap gui Edl2Chapters.exe "Edl2Chapters"
wrap console edl2chapters-cli.exe "Edl2Chapters command line"
cp -r "$RUNTIME_DIR" "$APP/runtime"

# 5. NSIS.
echo "Building the installer..."
makensis -V2 -DAPP_VERSION="$VERSION" -DAPP_DIR="$APP" -DICON_FILE="$ICON" \
    -DOUT_FILE="$OUT_DIR/Edl2Chapters-${VERSION}.exe" "$ROOT/packaging/windows/installer.nsi"

echo "Done: $OUT_DIR/Edl2Chapters-${VERSION}.exe"
