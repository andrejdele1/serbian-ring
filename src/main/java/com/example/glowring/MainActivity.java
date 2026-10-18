package com.example.glowring;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("ring", MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 96, 48, 48);

        TextView title = new TextView(this);
        title.setText("Serbian camera ring");
        title.setTextSize(24);
        root.addView(title);

        Button start = new Button(this);
        start.setText("Start ring");
        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
                Toast.makeText(this, "Allow 'Display over other apps', then tap Start again",
                        Toast.LENGTH_LONG).show();
                return;
            }
            startForegroundService(new Intent(this, OverlayService.class));
        });
        root.addView(start);

        Button stop = new Button(this);
        stop.setText("Stop ring");
        stop.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));
        root.addView(stop);

        addSeek(root, "Width (dp)", "w", 100, 30);
addSeek(root, "Height (dp)", "h", 50, 12);
        addSeek(root, "Thickness (dp)", "thick", 12, 3);
        addSeek(root, "Horizontal position (50 = center)", "x", 100, 50);
        addSeek(root, "Vertical position (dp from top)", "y", 60, 14);

        Button redetect = new Button(this);
        redetect.setText("Re-detect camera hole");
        redetect.setOnClickListener(v -> {
            sp.edit().remove("calibrated").apply();
            Toast.makeText(this, "Stop and Start the ring to re-detect", Toast.LENGTH_SHORT).show();
        });
        root.addView(redetect);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    private void addSeek(LinearLayout root, String label, String key, int max, int def) {
        TextView tv = new TextView(this);
        int cur = sp.getInt(key, def);
        tv.setText(label + ": " + cur);
        tv.setPadding(0, 40, 0, 0);
        root.addView(tv);

        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgress(cur);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int value, boolean fromUser) {
                sp.edit().putInt(key, value).apply();
                tv.setText(label + ": " + value);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(sb);
    }
}
