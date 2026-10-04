#!/usr/bin/env bash
# Minimal APK build without Gradle: aapt + javac + dx + zipalign + apksigner.
set -euo pipefail
cd "$(dirname "$0")"
ANDROID_JAR=${ANDROID_JAR:?set ANDROID_JAR to an API 33+ android.jar}
SRC=app/src/main
OUT=build
rm -rf $OUT && mkdir -p $OUT/gen $OUT/classes

aapt package -f -m -J $OUT/gen -M $SRC/AndroidManifest.xml -S $SRC/res -I $ANDROID_JAR -F $OUT/unsigned.apk
javac --release 8 -nowarn -encoding UTF-8 -classpath $ANDROID_JAR -d $OUT/classes \
  $(find $SRC/java $OUT/gen -name '*.java') 2>&1 | grep -v "warning: \[options\]" || true
dalvik-exchange --dex --output=$OUT/classes.dex $OUT/classes
(cd $OUT && zip -q -j unsigned.apk classes.dex)
zipalign -f 4 $OUT/unsigned.apk $OUT/aligned.apk

if [ ! -f debug.keystore ]; then
  keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
    -alias nxtqs -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=NXTPAPER Tiles" >/dev/null 2>&1
fi
apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android \
  --out $OUT/nxtpaper-tiles.apk $OUT/aligned.apk
apksigner verify $OUT/nxtpaper-tiles.apk && ls -la $OUT/nxtpaper-tiles.apk
