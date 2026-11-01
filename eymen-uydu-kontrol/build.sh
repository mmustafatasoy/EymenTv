#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${TV_ANDROID_JAR:?Set TV_ANDROID_JAR to Android API 33 android.jar}"
: "${TV_BUILD_TOOLS:?Set TV_BUILD_TOOLS to SDK build-tools directory}"
mkdir -p build/classes
if [ -n "${TV_ECJ_JAR:-}" ]; then
 java -jar "$TV_ECJ_JAR" -source 8 -target 8 -encoding UTF-8 -classpath "$TV_ANDROID_JAR" -d build/classes src/com/eymen/uydukontrol/*.java
else
 javac -source 8 -target 8 -encoding UTF-8 -classpath "$TV_ANDROID_JAR" -d build/classes src/com/eymen/uydukontrol/*.java
fi
"$TV_BUILD_TOOLS/aapt2" compile --dir res -o build/res.zip
"$TV_BUILD_TOOLS/aapt2" link -o build/unsigned.apk -I "$TV_ANDROID_JAR" --manifest AndroidManifest.xml -A assets build/res.zip
"$TV_BUILD_TOOLS/d8" --lib "$TV_ANDROID_JAR" --min-api 26 --output build build/classes/com/eymen/uydukontrol/*.class
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/unsigned.apk','a') as z:z.write('build/classes.dex','classes.dex')
PY
"$TV_BUILD_TOOLS/zipalign" -f 4 build/unsigned.apk build/aligned.apk
"$TV_BUILD_TOOLS/apksigner" sign --ks signing/development.jks --ks-pass pass:android --out build/EYMEN-TV-0.12.apk build/aligned.apk
"$TV_BUILD_TOOLS/apksigner" verify --verbose build/EYMEN-TV-0.12.apk
"$TV_BUILD_TOOLS/zipalign" -c 4 build/EYMEN-TV-0.12.apk
