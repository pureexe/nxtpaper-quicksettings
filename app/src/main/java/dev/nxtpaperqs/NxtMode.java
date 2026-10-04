package dev.nxtpaperqs;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.provider.Settings;
import android.service.quicksettings.TileService;
import android.util.Log;

/**
 * Switches the TCL NXTPAPER display mode the same way Settings > NXTPAPER zone does
 * (com.tct.nxtpaper.NxtpaperSettingsFragment#ApplySwitchMode):
 *   1. IThemeService.restoreToDefaultTheme(style)  (com.tcl.themehelper, exported service)
 *   2. Settings.Secure "nxtpaper_ink_style_state" = style  (NXTVISION applies the panel effect)
 *
 * style: 0 = Regular, 1 = Color Paper, 2 = Ink Paper.
 */
public final class NxtMode {
    private static final String TAG = "NxtMode";

    public static final int REGULAR = 0;
    public static final int COLOR_PAPER = 1;
    public static final int INK_PAPER = 2;

    static final String KEY_INK_STYLE = "nxtpaper_ink_style_state";
    static final String KEY_BOOK_STYLE = "nxtpaper_book_style_state"; // "Max ink" mode (3)

    private static final String THEME_PKG = "com.tcl.themehelper";
    private static final String THEME_CLS = "com.tcl.themehelper.ThemeService";
    private static final String THEME_DESCRIPTOR = "com.tcl.themehelper.IThemeService";
    private static final int TX_RESTORE_TO_DEFAULT_THEME = 6;

    private static HandlerThread sWorker;

    private NxtMode() {}

    public static int get(Context ctx) {
        return Settings.Secure.getInt(ctx.getContentResolver(), KEY_INK_STYLE, REGULAR);
    }

    private static final String PREFS = "prefs";
    private static final String PREF_APPLY_THEME = "apply_theme";

    /** Whether to also apply TCL's NXTPAPER theme, like Settings does. On by default. */
    public static boolean applyTheme(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PREF_APPLY_THEME, true);
    }

    public static void setApplyTheme(Context ctx, boolean on) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(PREF_APPLY_THEME, on).apply();
    }

    public static boolean hasPermission(Context ctx) {
        return ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static synchronized Handler worker() {
        if (sWorker == null) {
            sWorker = new HandlerThread("nxtmode");
            sWorker.start();
        }
        return new Handler(sWorker.getLooper());
    }

    /** Applies {@code style}. {@code done} (may be null) runs on the main thread afterwards. */
    public static void set(Context context, final int style, final Runnable done) {
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        final Runnable finish = new Runnable() {
            boolean ran;
            @Override public void run() {
                if (ran) return;
                ran = true;
                worker().post(new Runnable() {
                    @Override public void run() {
                        writeSetting(app, style);
                        if (done != null) main.post(done);
                        refreshTiles(app);
                    }
                });
            }
        };

        final int from = get(app);
        Intent i = new Intent().setComponent(new ComponentName(THEME_PKG, THEME_CLS)).setPackage(THEME_PKG);
        final ServiceConnection conn = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, final IBinder binder) {
                final ServiceConnection self = this;
                worker().post(new Runnable() {
                    @Override public void run() {
                        // TCL's theme replaces the wallpaper (and "Regular" restores the
                        // *default* theme wallpaper), so keep the user's own wallpaper
                        // when leaving Regular and put it back when returning.
                        if (from == REGULAR && style != REGULAR) {
                            WallpaperBackup.save(app);
                        }
                        restoreTheme(binder, style);
                        if (style == REGULAR) {
                            WallpaperBackup.restoreAfterTheme(app);
                        }
                        try { app.unbindService(self); } catch (Exception ignored) {}
                        main.post(finish);
                    }
                });
            }
            @Override public void onServiceDisconnected(ComponentName name) {}
        };

        if (!applyTheme(app)) {
            // Default: only switch the display mode; leave theme + wallpaper alone.
            main.post(finish);
            return;
        }
        if (!WallpaperBackup.canRead(app)) {
            // Applying TCL's theme without being able to save the wallpaper would lose it.
            Log.w(TAG, "theme option on but no wallpaper access; switching display mode only");
            main.post(new Runnable() {
                @Override public void run() {
                    android.widget.Toast.makeText(app,
                            "NXTPAPER theme skipped to protect your wallpaper. Long-press the tile to allow wallpaper access.",
                            android.widget.Toast.LENGTH_LONG).show();
                }
            });
            main.post(finish);
            return;
        }

        boolean bound = false;
        try {
            bound = app.bindService(i, conn, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.w(TAG, "bind theme service failed", e);
        }
        if (!bound) {
            Log.w(TAG, "theme service unavailable; applying display mode only");
            main.post(finish);
        } else {
            // Safety net: never hang if the service doesn't connect.
            main.postDelayed(finish, 4000);
        }
    }

    private static void restoreTheme(IBinder binder, int style) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(THEME_DESCRIPTOR);
            data.writeInt(style);
            binder.transact(TX_RESTORE_TO_DEFAULT_THEME, data, reply, 0);
            reply.readException();
            Log.i(TAG, "restoreToDefaultTheme(" + style + ") ok");
        } catch (Exception e) {
            Log.w(TAG, "restoreToDefaultTheme failed", e);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private static void writeSetting(Context app, int style) {
        try {
            Settings.Secure.putInt(app.getContentResolver(), KEY_BOOK_STYLE, 0);
            Settings.Secure.putInt(app.getContentResolver(), KEY_INK_STYLE, style);
            Log.i(TAG, KEY_INK_STYLE + "=" + style);
        } catch (SecurityException e) {
            Log.e(TAG, "WRITE_SECURE_SETTINGS not granted", e);
        }
    }

    /** Refreshes every UI surface (QS tiles + home-screen widgets) to the current mode. */
    static void refreshTiles(Context app) {
        TileService.requestListeningState(app, new ComponentName(app, ColorPaperTileService.class));
        TileService.requestListeningState(app, new ComponentName(app, InkPaperTileService.class));
        ModeWidgets.updateAll(app);
    }
}
