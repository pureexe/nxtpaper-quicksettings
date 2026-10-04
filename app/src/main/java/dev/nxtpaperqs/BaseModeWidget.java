package dev.nxtpaperqs;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/** 1x1 home-screen widget: tap to toggle {@link #targetMode()} on/off (off = Regular). */
public abstract class BaseModeWidget extends AppWidgetProvider {

    protected abstract int targetMode();

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        ModeWidgets.updateAll(ctx);
        ModeWatchJob.schedule(ctx);
    }

    @Override
    public void onEnabled(Context ctx) {
        ModeWatchJob.schedule(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!ModeWidgets.ACTION_TOGGLE.equals(intent.getAction())) {
            super.onReceive(ctx, intent);
            return;
        }
        if (!NxtMode.hasPermission(ctx)) {
            Toast.makeText(ctx, "Grant permission first (open NXTPAPER Tiles app)", Toast.LENGTH_LONG).show();
            ctx.startActivity(new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        int current = NxtMode.get(ctx);
        int next = current == targetMode() ? NxtMode.REGULAR : targetMode();

        // Optimistic redraw of both widgets, then apply.
        ModeWidgets.update(ctx, ColorPaperWidget.class, NxtMode.COLOR_PAPER, next);
        ModeWidgets.update(ctx, InkPaperWidget.class, NxtMode.INK_PAPER, next);

        final PendingResult pr = goAsync();
        NxtMode.set(ctx, next, new Runnable() {
            @Override public void run() { pr.finish(); }
        });
    }
}
