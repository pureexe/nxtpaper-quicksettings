package dev.nxtpaperqs;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

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
        theme.setText("Also apply NXTPAPER theme (changes wallpaper)");
        theme.setChecked(NxtMode.applyTheme(this));
        theme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) {
                NxtMode.setApplyTheme(MainActivity.this, on);
            }
        });
        root.addView(theme);

        root.addView(button("Regular", NxtMode.REGULAR));
        root.addView(button("Color Paper", NxtMode.COLOR_PAPER));
        root.addView(button("Ink Paper", NxtMode.INK_PAPER));
        setContentView(root);
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
              + "or long-press the home screen > Widgets > NXTPAPER Tiles.");
        } else {
            sb.append("Permission: NOT granted. Run once from a PC:\n\n")
              .append("adb shell pm grant ").append(getPackageName())
              .append(" android.permission.WRITE_SECURE_SETTINGS");
        }
        status.setText(sb);
    }
}
