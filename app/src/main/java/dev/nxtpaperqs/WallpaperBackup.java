package dev.nxtpaperqs;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Saves the user's wallpaper before TCL's NXTPAPER theme replaces it, and restores it when
 * going back to Regular mode (where TCL would otherwise apply the default theme wallpaper).
 *
 * Reading the current wallpaper needs BOTH "All files access" (MANAGE_EXTERNAL_STORAGE) and
 * READ_MEDIA_IMAGES. Without them Android silently hands back the *default* wallpaper instead
 * of throwing, so {@link #canRead} must be checked before trusting a backup:
 *   adb shell appops set dev.nxtpaperqs MANAGE_EXTERNAL_STORAGE allow
 *   adb shell pm grant dev.nxtpaperqs android.permission.READ_MEDIA_IMAGES
 */
final class WallpaperBackup {
    private static final String TAG = "WallpaperBackup";
    private static final String SYSTEM = "wallpaper_system";
    private static final String LOCK = "wallpaper_lock";

    private WallpaperBackup() {}

    static boolean canRead(Context ctx) {
        return Environment.isExternalStorageManager()
                && ctx.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private static File dir(Context ctx) {
        File d = ctx.getExternalFilesDir("wallpaper-backup"); // also reachable via adb / file manager
        if (d == null) d = new File(ctx.getFilesDir(), "wallpaper-backup");
        d.mkdirs();
        return d;
    }

    static boolean hasBackup(Context ctx) {
        return new File(dir(ctx), SYSTEM).length() > 0;
    }

    /** Copies the current home (and separate lock, if any) wallpaper. Returns true on success. */
    static boolean save(Context ctx) {
        if (!canRead(ctx)) {
            // Would silently read the default wallpaper -> never save that as "the user's".
            Log.w(TAG, "no wallpaper read access; not saving");
            return false;
        }
        WallpaperManager wm = WallpaperManager.getInstance(ctx);
        if (wm.getWallpaperInfo() != null) {
            Log.w(TAG, "live wallpaper in use; cannot back it up");
            return false;
        }
        File d = dir(ctx);
        File sys = new File(d, SYSTEM), lock = new File(d, LOCK);
        try {
            if (!copy(wm.getWallpaperFile(WallpaperManager.FLAG_SYSTEM), new File(d, SYSTEM + ".tmp"))) {
                Log.w(TAG, "no system wallpaper file");
                return false;
            }
            new File(d, SYSTEM + ".tmp").renameTo(sys);
            // Lock screen only has its own file if it differs from the home screen.
            if (!copy(wm.getWallpaperFile(WallpaperManager.FLAG_LOCK), lock)) lock.delete();
            Log.i(TAG, "saved wallpaper (" + sys.length() + " bytes, lock=" + lock.exists() + ")");
            return true;
        } catch (SecurityException e) {
            Log.w(TAG, "no permission to read wallpaper (grant All files access)", e);
            return false;
        } catch (Exception e) {
            Log.w(TAG, "wallpaper backup failed", e);
            return false;
        }
    }

    /**
     * Waits until ThemeHelper has finished applying its theme (it sets the wallpaper
     * asynchronously), then restores the saved wallpaper. Runs on its own thread.
     */
    static void restoreAfterTheme(final Context ctx) {
        if (!hasBackup(ctx)) return;
        new Thread(new Runnable() {
            @Override public void run() {
                waitForThemeApplied(ctx);
                restore(ctx);
            }
        }, "wallpaper-restore").start();
    }

    private static void waitForThemeApplied(Context ctx) {
        long deadline = System.currentTimeMillis() + 20000;
        // Give ThemeHelper a moment to flag that it's applying.
        sleep(500);
        while (System.currentTimeMillis() < deadline
                && Settings.Secure.getInt(ctx.getContentResolver(), "nxtpaper_is_applying_theme", 0) != 0) {
            sleep(250);
        }
        sleep(1500); // the wallpaper is set right after the flag clears
    }

    static boolean restore(Context ctx) {
        File d = dir(ctx);
        File sys = new File(d, SYSTEM), lock = new File(d, LOCK);
        if (sys.length() == 0) return false;
        WallpaperManager wm = WallpaperManager.getInstance(ctx);
        try {
            if (lock.length() > 0) {
                try (InputStream in = new FileInputStream(sys)) {
                    wm.setStream(in, null, true, WallpaperManager.FLAG_SYSTEM);
                }
                try (InputStream in = new FileInputStream(lock)) {
                    wm.setStream(in, null, true, WallpaperManager.FLAG_LOCK);
                }
            } else {
                try (InputStream in = new FileInputStream(sys)) {
                    wm.setStream(in, null, true, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
                }
            }
            Log.i(TAG, "restored wallpaper");
            return true;
        } catch (Exception e) {
            Log.w(TAG, "wallpaper restore failed", e);
            return false;
        }
    }

    private static boolean copy(ParcelFileDescriptor pfd, File out) throws Exception {
        if (pfd == null) return false;
        try (InputStream in = new FileInputStream(pfd.getFileDescriptor());
             OutputStream os = new FileOutputStream(out)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            long total = 0;
            while ((n = in.read(buf)) > 0) { os.write(buf, 0, n); total += n; }
            return total > 0;
        } finally {
            pfd.close();
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
