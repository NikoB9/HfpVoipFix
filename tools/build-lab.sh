#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_PLATFORM_JAR:?Path to Android 30 android.jar}"
: "${ANDROID_BUILD_TOOLS:?Path to build tools 35.0.0}"
: "${XPOSED_API_JAR:?Path to compile-only Xposed API 82 jar}"
: "${JAVA_HOME:?JDK 17 required}"
export PATH="$JAVA_HOME/bin:$PATH"
export LD_LIBRARY_PATH="$JAVA_HOME/lib:$JAVA_HOME/lib/server:$ANDROID_BUILD_TOOLS/lib64:${LD_LIBRARY_PATH:-}"
OUT=build-lab
mkdir -p "$OUT/classes" "$OUT/dex"
find "$OUT/classes" "$OUT/dex" -type f -delete
find app/src/main/java -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 -source 8 -target 8 -Xlint:deprecation -cp "$ANDROID_PLATFORM_JAR:$XPOSED_API_JAR" -d "$OUT/classes" @"$OUT/sources.txt"
jar cf "$OUT/app.jar" -C "$OUT/classes" .
"$ANDROID_BUILD_TOOLS/d8" --release --min-api 30 --lib "$ANDROID_PLATFORM_JAR" --lib "$XPOSED_API_JAR" --output "$OUT/dex" "$OUT/app.jar"
"$ANDROID_BUILD_TOOLS/aapt2" compile --dir app/src/main/res -o "$OUT/res.zip"
"$ANDROID_BUILD_TOOLS/aapt2" link -I "$ANDROID_PLATFORM_JAR" --manifest app/src/main/AndroidManifest.xml --min-sdk-version 30 --target-sdk-version 30 -o "$OUT/unsigned.apk" "$OUT/res.zip"
(cd "$OUT/dex" && zip -q -0 ../unsigned.apk classes.dex)
(cd app/src/main && zip -q -r "../../../$OUT/unsigned.apk" assets)
"$ANDROID_BUILD_TOOLS/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
if [[ -n "${LAB_KEYSTORE:-}" ]]; then
  : "${LAB_KEYSTORE_PASSWORD:?Signing password required}"
  mkdir -p "$OUT/signer"
  javac -encoding UTF-8 -Xlint:deprecation -cp "$ANDROID_BUILD_TOOLS/lib/apksigner.jar" -d "$OUT/signer" tools/SignApk.java
  "$JAVA_HOME/bin/java" -cp "$OUT/signer:$ANDROID_BUILD_TOOLS/lib/apksigner.jar" SignApk "$OUT/aligned.apk" "$OUT/HfpVoipLab-1.7.7.apk"
  "$JAVA_HOME/bin/java" -jar "$ANDROID_BUILD_TOOLS/lib/apksigner.jar" verify --verbose --print-certs "$OUT/HfpVoipLab-1.7.7.apk" > "$OUT/signature.txt"
  "$ANDROID_BUILD_TOOLS/zipalign" -c -v 4 "$OUT/HfpVoipLab-1.7.7.apk" > "$OUT/alignment.txt"
  "$ANDROID_BUILD_TOOLS/aapt2" dump badging "$OUT/HfpVoipLab-1.7.7.apk" > "$OUT/manifest.txt"
  test -s "$OUT/signature.txt"
  sha256sum "$OUT/HfpVoipLab-1.7.7.apk"
else
  echo 'Unsigned build only: stable signing key is required for an installable release.'
fi
