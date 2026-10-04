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
2. From a computer with [ADB](https://developer.android.com/tools/adb) set up, grant the permissions. You only do this once:

   ```sh
   # Required: lets the app switch the NXTPAPER mode
   adb shell pm grant dev.nxtpaperqs android.permission.WRITE_SECURE_SETTINGS

   # Recommended: lets the app save your wallpaper before TCL's theme replaces it (see below)
   adb shell pm grant dev.nxtpaperqs android.permission.READ_MEDIA_IMAGES
   adb shell appops set dev.nxtpaperqs MANAGE_EXTERNAL_STORAGE allow
   ```

   None of this is root. You can revoke any of it with `pm revoke` or `appops set ... default`, or by uninstalling the app.
3. Add the controls you want:
   - **Tiles:** pull down Quick Settings → ✏️ Edit → drag **Color Paper** and **Ink Paper** into place.
   - **Widgets:** long-press the home screen → Widgets → **NXTPAPER Tiles** → drag the 1×1 **Color Paper** or **Ink Paper** widget onto your home screen.

### Settings: long-press a tile

The app has **no icon in the app drawer** on purpose. To open its settings, **long-press the Color Paper or Ink Paper tile** in Quick Settings. The settings screen shows the current mode and which permissions are granted. It also has the theme option below and test buttons for each mode.

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

### TCL's NXTPAPER theme (on by default)

When you pick a mode in Settings, TCL also applies a matching "Colorpaper/Einkpaper" theme through `com.tcl.themehelper` (`IThemeService.restoreToDefaultTheme`). The app does the same by default, so the tiles behave like the stock Settings page.

**That theme replaces your wallpaper**, and switching back to Regular applies TCL's *default* wallpaper rather than yours. So the app saves your wallpaper when you leave Regular mode and puts it back when you return. To read your current wallpaper, Android needs the two recommended permissions from the install steps. You can also tap **Allow wallpaper access** on the settings screen. Without them, Android silently returns the default wallpaper instead of yours. The app then skips the theme and only switches the display mode, rather than lose your wallpaper. Live wallpapers can't be saved this way.

**To turn the theme off**, long-press a tile and untick **"Also apply NXTPAPER theme"**. The tiles and widgets will then only switch the display mode and won't touch your theme or wallpaper.

## FAQ

**Can I turn off USB debugging or Developer options after granting the permission?**

Yes. The `pm grant` permission is saved with the app, so it stays granted after you turn off USB debugging or Developer options, and after reboots. You only need ADB once, to grant it. You'd need to grant it again only if you:

- uninstall and reinstall the app (updating to a new version keeps it), or
- factory-reset the tablet.

If you want to grant the wallpaper permissions over ADB too, do that before turning debugging off. Otherwise you can grant them later from the settings screen (long-press a tile), without ADB.

To check, long-press a tile after turning debugging off. The settings screen should say **"Permission: granted ✓"**.

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
