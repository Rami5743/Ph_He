#!/usr/bin/env bash
# Builds and signs he-ph1-keyboard.apk with the Android SDK command line tools
# only — no Gradle, no network, no third-party dependency.
#
#   ANDROID_HOME=/path/to/android-sdk ./android/build.sh
#
# Needs: a JDK, build-tools >= 35 and platforms/android-34 of the SDK.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$HERE")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"
# newest installed build-tools; d8 before 35 crashes on anonymous classes
# when it runs on a JDK 21
BT="$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)"
PLATFORM="$SDK/platforms/android-34/android.jar"
OUT="$HERE/build"
APK="$ROOT/dist/he-ph1-keyboard.apk"

KEYSTORE="$HERE/keystore/he-ph1.jks"
KEYALIAS=heph1
KEYPASS=heph1keyboard

for f in "$BT/aapt2" "$BT/d8" "$BT/zipalign" "$BT/apksigner" "$PLATFORM"; do
  [ -e "$f" ] || { echo "missing: $f" >&2; exit 1; }
done

rm -rf "$OUT"
mkdir -p "$OUT/res" "$OUT/gen" "$OUT/classes" "$OUT/dex" "$ROOT/dist"

echo "== aapt2 compile"
"$BT/aapt2" compile --dir "$HERE/res" -o "$OUT/res/resources.zip"

echo "== aapt2 link"
"$BT/aapt2" link \
  -o "$OUT/base.apk" \
  -I "$PLATFORM" \
  --manifest "$HERE/AndroidManifest.xml" \
  --java "$OUT/gen" \
  --min-sdk-version 21 \
  --target-sdk-version 34 \
  "$OUT/res/resources.zip"

echo "== javac"
find "$HERE/src" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 -source 8 -target 8 -nowarn \
  -bootclasspath "$PLATFORM" -classpath "$PLATFORM" \
  -d "$OUT/classes" @"$OUT/sources.txt" 2>&1 | grep -v 'bootstrap class path' || true

echo "== d8"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
"$BT/d8" --lib "$PLATFORM" --min-api 21 --output "$OUT/dex" @"$OUT/classes.txt"

echo "== package"
cp "$OUT/base.apk" "$OUT/unaligned.apk"
( cd "$OUT/dex" && zip -q -X "$OUT/unaligned.apk" classes.dex )

echo "== zipalign"
"$BT/zipalign" -f 4 "$OUT/unaligned.apk" "$OUT/aligned.apk"

if [ ! -f "$KEYSTORE" ]; then
  echo "== keystore"
  mkdir -p "$(dirname "$KEYSTORE")"
  keytool -genkeypair -v -keystore "$KEYSTORE" -alias "$KEYALIAS" \
    -keyalg RSA -keysize 2048 -validity 10950 \
    -storepass "$KEYPASS" -keypass "$KEYPASS" \
    -dname "CN=he_ph1 keyboard, OU=Ph_He, O=Ph_He, C=IL"
fi

echo "== apksigner"
"$BT/apksigner" sign \
  --ks "$KEYSTORE" --ks-key-alias "$KEYALIAS" \
  --ks-pass "pass:$KEYPASS" --key-pass "pass:$KEYPASS" \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out "$APK" "$OUT/aligned.apk"

"$BT/apksigner" verify --print-certs "$APK"
echo
echo "APK: $APK"
ls -l "$APK"
