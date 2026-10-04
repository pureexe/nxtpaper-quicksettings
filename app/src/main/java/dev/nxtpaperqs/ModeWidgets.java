package dev.nxtpaperqs;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Renders the 1x1 Color Paper / Ink Paper widgets in an on/off style like a QS tile. */
final class ModeWidgets {
    static final String ACTION_TOGGLE = "dev.nxtpaperqs.action.TOGGLE";

    private ModeWidgets() {}

    static void updateAll(Context ctx) {
        int mode = NxtMode.get(ctx);
        update(ctx, ColorPaperWidget.class, NxtMode.COLOR_PAPER, mode);
        update(ctx, InkPaperWidget.class, NxtMode.INK_PAPER, mode);
    }

    /** Draw a provider's widgets as if the current mode were {@code mode}. */
    static void update(Context ctx, Class<?> provider, int target, int mode) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        ComponentName cn = new ComponentName(ctx, provider);
        int[] ids = mgr.getAppWidgetIds(cn);
        if (ids == null || ids.length == 0) return;
        mgr.updateAppWidget(cn, build(ctx, provider, target, mode == target));
    }

    private static RemoteViews build(Context ctx, Class<?> provider, int target, boolean on) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_mode);
        boolean color = target == NxtMode.COLOR_PAPER;

        v.setImageViewResource(R.id.icon, color ? R.drawable.ic_color_paper : R.drawable.ic_ink_paper);
        v.setTextViewText(R.id.label, ctx.getString(color ? R.string.tile_color : R.string.tile_ink));
        v.setTextViewText(R.id.state, on ? "On" : "Off");
        v.setInt(R.id.root, "setBackgroundResource", on ? R.drawable.widget_bg_on : R.drawable.widget_bg_off);

        // Resolved by the launcher at render time, so light/dark + wallpaper colors stay correct.
        int fg = on ? R.color.widget_fg_on : R.color.widget_fg_off;
        v.setColorStateList(R.id.icon, "setImageTintList", fg);
        v.setColorStateList(R.id.label, "setTextColor", fg);
        v.setColorStateList(R.id.state, "setTextColor", fg);
        v.setContentDescription(R.id.root,
                ctx.getString(color ? R.string.tile_color : R.string.tile_ink) + (on ? ", on" : ", off"));

        Intent i = new Intent(ctx, provider).setAction(ACTION_TOGGLE);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, target, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.root, pi);
        return v;
    }
}
