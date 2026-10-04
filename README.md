# NXTPAPER Quick Settings

Quick Settings tiles and 1×1 home-screen widgets that switch the **Color Paper** and **Ink Paper** display modes on TCL NXTPAPER tablets, so you don't have to open **Settings → NXTPAPER zone** every time.

| Action | Result |
|---|---|
| Tap **Color Paper** | Switches to Color Paper mode |
| Tap **Ink Paper** | Switches to Ink Paper mode (and turns Color Paper off) |
| Tap the active tile or widget again | Goes back to Regular mode |

The tiles and widgets also stay in sync when you change the mode somewhere else (Settings, the NXTPAPER key, or ADB).

> Tested on the **TCL NXTPAPER 11 Plus** (model 9469X, Android 15). Other NXTPAPER devices may work if they use the same setting (see [How it works](#how-it-works)). Reports are welcome.

## Demo

[![Watch the demo on YouTube](https://img.youtube.com/vi/6324D_OUpOs/hqdefault.jpg)](https://youtu.be/6324D_OUpOs)

▶️ [Watch how to install and use it on YouTube](https://youtu.be/6324D_OUpOs)

## Install

1. Download the APK from [Releases](../../releases) and install it. Android may warn that the app isn't from the Play Store. Choose **Install anyway**.
2. Grant the one permission it needs. You only do this once, from a computer with [ADB](https://developer.android.com/tools/adb) set up:

   ```sh
   adb shell pm grant dev.nxtpaperqs android.permission.WRITE_SECURE_SETTINGS
   ```

   This lets the app change the system setting behind the NXTPAPER mode. It isn't root, and you can revoke it at any time with `pm revoke` or by uninstalling the app.
3. Add the controls you want:
   - **Tiles:** pull down Quick Settings → ✏️ Edit → drag **Color Paper** and **Ink Paper** into place.
   - **Widgets:** long-press the home screen → Widgets → **NXTPAPER Tiles** → drag the 1×1 **Color Paper** or **Ink Paper** widget onto your home screen.

Opening the app shows the current mode, whether the permission is granted, and test buttons for each mode.

## How it works

The NXTPAPER mode is stored in one system setting:

```
Settings.Secure  nxtpaper_ink_style_state
  0 = Regular
  1 = Color Paper
  2 = Ink Paper
```

TCL's display service (`com.tct.iris` / NXTVISION) watches this value and applies the matching screen effect. So you can switch modes with ADB alone, without this app:

```sh
adb shell settings put secure nxtpaper_ink_style_state 1   # Color Paper
adb shell settings put secure nxtpaper_ink_style_state 2   # Ink Paper
adb shell settings put secure nxtpaper_ink_style_state 0   # Regular
adb shell settings get secure nxtpaper_ink_style_state     # current mode
```

I found the key by comparing `adb shell settings list secure` output in each mode. I then confirmed it in the stock Settings app, where `com.tct.nxtpaper.NxtpaperSettingsFragment` writes 0, 1 or 2 to it. As far as I can tell it isn't documented anywhere else.

### Optional: TCL's NXTPAPER theme

When you pick a mode in Settings, TCL also applies a matching "Colorpaper/Einkpaper" theme through `com.tcl.themehelper` (`IThemeService.restoreToDefaultTheme`). **That theme replaces your wallpaper**, so the app leaves it off by default and only changes the display mode. If you want the full stock behavior, turn on **"Also apply NXTPAPER theme (changes wallpaper)"** in the app.

## Build

The project builds without Gradle, using only the basic Android build tools. On Debian/Ubuntu:

```sh
sudo apt install aapt apksigner zipalign dalvik-exchange openjdk-17-jdk
# Any android.jar from API 33 or newer, e.g. from the Android SDK: platforms/android-33/android.jar
ANDROID_JAR=/path/to/android.jar ./build.sh
```

The script compiles, dexes, aligns and signs the APK with a local debug key, and writes it to `build/nxtpaper-tiles.apk`.

```
app/src/main/java/dev/nxtpaperqs/
  NxtMode.java              reads/writes the mode (+ optional theme call)
  BaseModeTile.java         shared Quick Settings tile logic
  ColorPaperTileService / InkPaperTileService
  BaseModeWidget.java       shared 1x1 widget logic
  ColorPaperWidget / InkPaperWidget
  ModeWidgets.java          renders widget on/off state
  ModeWatchJob.java         content-trigger job that keeps tiles/widgets in sync
  MainActivity.java         status + permission help + test buttons
```

## Disclaimer

This is an unofficial project and isn't affiliated with or endorsed by TCL. NXTPAPER is a trademark of TCL. It changes an undocumented system setting, so a future firmware update could break it. Use at your own risk.

## License

[MIT](LICENSE)
