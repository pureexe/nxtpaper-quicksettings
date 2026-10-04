package dev.nxtpaperqs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    private TextView wallpaperNote;
    private Button wallpaperAccess;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        status = new TextView(this);
        status.setTextSize(16);
        status.setTextIsSelectable(true);
        root.addView(status);

        final CheckBox theme = new CheckBox(this);
        theme.setText("Also apply NXTPAPER theme (like Settings does)");
        theme.setChecked(NxtMode.applyTheme(this));
        theme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) {
                NxtMode.setApplyTheme(MainActivity.this, on);
                refresh();
            }
        });
        root.addView(theme);

        wallpaperNote = new TextView(this);
        wallpaperNote.setTextIsSelectable(true);
        root.addView(wallpaperNote);

        wallpaperAccess = new Button(this);
        wallpaperAccess.setText("Allow wallpaper access");
        wallpaperAccess.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 1);
                    return;
                }
                openAllFilesAccess();
            }
        });
        root.addView(wallpaperAccess);


        root.addView(button("Regular", NxtMode.REGULAR));
        root.addView(button("Color Paper", NxtMode.COLOR_PAPER));
        root.addView(button("Ink Paper", NxtMode.INK_PAPER));
        setContentView(root);
    }

    private void openAllFilesAccess() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        refresh();
        if (results.length > 0 && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
                && !android.os.Environment.isExternalStorageManager()) {
            openAllFilesAccess();
        }
    }

    private Button button(String label, final int mode) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                NxtMode.set(MainActivity.this, mode, new Runnable() {
                    @Override public void run() { refresh(); }
                });
            }
        });
        return btn;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ModeWatchJob.schedule(this);
        refresh();
    }

    private void refresh() {
        String[] names = {"Regular", "Color Paper", "Ink Paper", "Max Ink"};
        int m = NxtMode.get(this);
        StringBuilder sb = new StringBuilder();
        sb.append("Current mode: ").append(m >= 0 && m < names.length ? names[m] : String.valueOf(m)).append("\n\n");
        if (NxtMode.hasPermission(this)) {
            sb.append("Permission: granted ✓\n\nAdd the \"Color Paper\" and \"Ink Paper\" tiles from the Quick Settings edit screen, "
              + "or long-press the home screen > Widgets > NXTPAPER Tiles.\n\n"
              + "This app has no icon in the app drawer. Long-press either tile to come back to these settings.");
        } else {
            sb.append("Permission: NOT granted. Run once from a PC:\n\n")
              .append("adb shell pm grant ").append(getPackageName())
              .append(" android.permission.WRITE_SECURE_SETTINGS");
        }
        status.setText(sb);

        boolean theme = NxtMode.applyTheme(this);
        boolean canRead = WallpaperBackup.canRead(this);
        wallpaperNote.setVisibility(theme ? View.VISIBLE : View.GONE);
        wallpaperAccess.setVisibility(theme && !canRead ? View.VISIBLE : View.GONE);
        if (theme) {
            if (!canRead) {
                wallpaperNote.setText("TCL's theme replaces your wallpaper. To save it and put it back when you return "
                        + "to Regular, allow \"Photos\" and \"All files access\" (used only to read the current "
                        + "wallpaper). Until then the theme is skipped. Tap below, or run:\n\n"
                        + "adb shell pm grant " + getPackageName() + " android.permission.READ_MEDIA_IMAGES\n"
                        + "adb shell appops set " + getPackageName() + " MANAGE_EXTERNAL_STORAGE allow");
            } else if (m != NxtMode.REGULAR && !WallpaperBackup.hasBackup(this)) {
                wallpaperNote.setText("Wallpaper backup: none yet. It is saved the next time you switch from Regular "
                        + "to a paper mode.");
            } else {
                wallpaperNote.setText("Wallpaper backup: on ✓ (your wallpaper is saved when leaving Regular "
                        + "and restored when you come back).");
            }
        }
    }
}
