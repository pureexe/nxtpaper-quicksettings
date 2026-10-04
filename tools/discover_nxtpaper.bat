@echo off
REM Captures NXTPAPER settings in each mode so Claude can find which keys change.
REM Run from this folder with the tablet connected via ADB (adb in PATH).
cd /d "%~dp0"
if not exist dumps mkdir dumps
adb devices > dumps\devices.txt
adb shell getprop ro.product.model > dumps\model.txt
adb shell getprop ro.build.version.release >> dumps\model.txt
adb shell getprop ro.build.version.sdk >> dumps\model.txt
adb shell pm list packages -f > dumps\packages.txt

for %%M in (regular color ink) do (
  echo.
  echo ===== On the tablet, open Settings ^> NXTPAPER zone and select %%M mode. =====
  pause
  adb shell settings list system > dumps\%%M_system.txt
  adb shell settings list secure > dumps\%%M_secure.txt
  adb shell settings list global > dumps\%%M_global.txt
  adb shell dumpsys display > dumps\%%M_display.txt
  adb shell dumpsys SurfaceFlinger > dumps\%%M_sf.txt
  adb shell screencap -p /sdcard/nxt_%%M.png
  adb pull /sdcard/nxt_%%M.png dumps\%%M.png
)

REM Also record the Settings activity that shows the NXTPAPER zone page
echo.
echo ===== Now leave the NXTPAPER zone page OPEN on screen. =====
pause
adb shell dumpsys activity activities > dumps\activities.txt
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml dumps\ui.xml
echo Done. Tell Claude the dumps are ready.
pause
