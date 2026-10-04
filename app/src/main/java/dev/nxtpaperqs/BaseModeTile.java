package dev.nxtpaperqs;

import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

/** A tile that is ON when the NXTPAPER mode equals {@link #targetMode()}. */
public abstract class BaseModeTile extends TileService {

    protected abstract int targetMode();

    private final ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) { updateTile(); }
    };

    @Override
    public void onStartListening() {
        super.onStartListening();
        Uri uri = Settings.Secure.getUriFor(NxtMode.KEY_INK_STYLE);
        getContentResolver().registerContentObserver(uri, false, observer);
        updateTile();
    }

    @Override
    public void onStopListening() {
        getContentResolver().unregisterContentObserver(observer);
        super.onStopListening();
    }

    @Override
    public void onClick() {
        if (!NxtMode.hasPermission(this)) {
            Toast.makeText(this, "Grant permission first (open NXTPAPER Tiles app)", Toast.LENGTH_LONG).show();
            Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(i);
            return;
        }
        boolean on = NxtMode.get(this) == targetMode();
        int next = on ? NxtMode.REGULAR : targetMode();

        // Optimistic UI update
        Tile t = getQsTile();
        if (t != null) {
            t.setState(on ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
            t.updateTile();
        }
        NxtMode.set(this, next, null);
    }

    private void updateTile() {
        Tile t = getQsTile();
        if (t == null) return;
        boolean on = NxtMode.get(this) == targetMode();
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.updateTile();
    }
}
