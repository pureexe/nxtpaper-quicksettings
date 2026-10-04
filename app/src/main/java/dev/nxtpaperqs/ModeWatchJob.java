package dev.nxtpaperqs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/**
 * Wakes up whenever Settings.Secure "nxtpaper_ink_style_state" changes (from Settings,
 * the hardware key, NXTVISION, adb...) so widgets/tiles stay in sync without a running service.
 * Content-trigger jobs fire once, so the job re-schedules itself each run.
 */
public class ModeWatchJob extends JobService {
    private static final int JOB_ID = 0x4E58; // "NX"

    static void schedule(Context ctx) {
        JobScheduler js = ctx.getSystemService(JobScheduler.class);
        if (js == null) return;
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(ctx, ModeWatchJob.class))
                .addTriggerContentUri(new JobInfo.TriggerContentUri(
                        Settings.Secure.getUriFor(NxtMode.KEY_INK_STYLE), 0))
                .setTriggerContentUpdateDelay(100)
                .setTriggerContentMaxDelay(1000)
                .build();
        js.schedule(job);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        NxtMode.refreshTiles(getApplicationContext());
        schedule(this);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    /** Re-arm the watcher after reboot / app update. */
    public static class Rearm extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            schedule(ctx);
            ModeWidgets.updateAll(ctx);
        }
    }
}
