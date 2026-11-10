#!/bin/bash
# Takes the Play Store screenshots with sample data (your own data is not used or changed).
#
#   1. Start an emulator in Android Studio (Device Manager ▶), e.g. a Pixel phone.
#   2. In Terminal, from the project folder:   ./scripts/take-screenshots.sh
#
# The PNG files are saved in "ToBeDone Android Screenshots" on your Desktop.
# The sample data is the same as in the iPhone app's screenshots, plus Journaling and Shopping.
# It only exists in memory while the screenshot is taken: your real tasks stay as they are.

set -e
cd "$(dirname "$0")/.."

# adb comes with Android Studio; use it from the SDK if it isn't on the PATH
if ! command -v adb >/dev/null 2>&1; then
  export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
fi
command -v adb >/dev/null 2>&1 || { echo "adb not found. Install 'Android SDK Platform-Tools' in Android Studio › Settings › Android SDK › SDK Tools."; exit 1; }

# Use Android Studio's own Java if JAVA_HOME isn't set (Gradle needs Java 17–24)
if [ -z "$JAVA_HOME" ] && [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi

PKG="$(grep -o 'applicationId = "[^"]*"' app/build.gradle.kts | head -1 | cut -d'"' -f2)"
OUT="$HOME/Desktop/ToBeDone Android Screenshots"

echo "Waiting for the emulator..."
adb wait-for-device
if [ "$(adb devices | grep -c 'device$')" -gt 1 ]; then
  echo "More than one device is connected. Keep only the emulator you want, or set ANDROID_SERIAL."
  exit 1
fi

echo "Building and installing the debug version (takes a few minutes the first time)..."
./gradlew -q installDebug

# No permission popup during the screenshots
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null || true

# Clean status bar: 9:41, full battery, full Wi-Fi, no notification icons
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile show -e level 4 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null

rm -rf "$OUT"
mkdir -p "$OUT"

# file name | sample-data scene | screen to open
SHOTS=(
  "1-Home|home|HOME"
  "2-ToDo|todo|TODO"
  "3-DailyRoutine-Monthly|routine|ROUTINE"
  "4-DailyRoutine-Weekly|routine|WEEK"
  "5-MonthlyExpenses|monthly|MONTHLY"
  "6-Projects|projects|PROJECTS"
  "7-Shopping|shopping|SHOPPING"
  "8-Grocery|grocery|GROCERY"
  "9-Confidence|confidence|CONFIDENCE"
  "10-FocusTest|focus|FOCUS"
  "11-Journaling|journal|JOURNAL"
)

for shot in "${SHOTS[@]}"; do
  IFS='|' read -r NAME SCENE SCREEN <<< "$shot"
  echo "  $NAME"
  adb shell am force-stop "$PKG"
  adb shell am start -W -n "$PKG/.MainActivity" --es demoScene "$SCENE" --es open "$SCREEN" >/dev/null
  sleep 3   # let the screen finish drawing
  adb exec-out screencap -p > "$OUT/$NAME.png"
done

adb shell am force-stop "$PKG"
adb shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null

echo "Done: $OUT"
open "$OUT" 2>/dev/null || true
