package com.example.glowring;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private SharedPreferences sp;
    private float d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("ring", MODE_PRIVATE);
        d = getResources().getDisplayMetrics().density;

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
        buildUi();
    }

    private void buildUi() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(48, 96, 48, 0);

        TextView title = new TextView(this);
        title.setText("Serbian camera ring");
        title.setTextSize(24);
        outer.addView(title);

        // Preview stays pinned at the top while you scroll the controls.
        OverlayService.RingView preview = new OverlayService.RingView(this, true);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.round(150 * d));
        plp.topMargin = Math.round(12 * d);
        outer.addView(preview, plp);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, 48);

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

        header(root, "Shape");
        addSeek(root, "Width (dp)", "w", 500, 30);
        addSeek(root, "Height (dp)", "h", 300, 12);
        addSeek(root, "Corner roundness (0 = rectangle, 100 = pill)", "round", 100, 100);
        addSeek(root, "Rotation (degrees)", "rot", 360, 0);
        addSeek(root, "Thickness (dp)", "thick", 30, 3);

        header(root, "Animation");
        addSeek(root, "Spin speed (0 = still)", "spin", 100, 30);

        header(root, "Position");
        SeekBar sx = addSeek(root, "Horizontal (300 = center)", "px", 600, 300);
        SeekBar sy = addSeek(root, "Vertical (dp from top)", "py", 1000, 14);

        TextView fine = new TextView(this);
        fine.setText("Fine tune (1 dp per tap)");
        fine.setPadding(0, 32, 0, 0);
        root.addView(fine);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        nudgeButton(row, "\u25C0", sx, -1);
        nudgeButton(row, "\u25B6", sx, 1);
        nudgeButton(row, "\u25B2", sy, -1);
        nudgeButton(row, "\u25BC", sy, 1);
        root.addView(row);

        Button reset = new Button(this);
        reset.setText("Reset to detected camera hole");
        reset.setOnClickListener(v -> {
            sp.edit().clear().apply();
            stopService(new Intent(this, OverlayService.class));
            if (Settings.canDrawOverlays(this)) {
                startForegroundService(new Intent(this, OverlayService.class));
            }
            Toast.makeText(this, "Resetting...", Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(this::buildUi, 1200);
        });
        root.addView(reset);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        outer.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(outer);
    }

    private void header(LinearLayout root, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setTextColor(0xFF00796B);
        tv.setPadding(0, 56, 0, 0);
        root.addView(tv);
    }

    private SeekBar addSeek(LinearLayout root, String label, String key, int max, int def) {
        TextView tv = new TextView(this);
        int cur = Math.min(sp.getInt(key, def), max);
        tv.setText(label + ": " + cur);
        tv.setPadding(0, 32, 0, 0);
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
        return sb;
    }

    private void nudgeButton(LinearLayout row, String text, SeekBar sb, int delta) {
        Button b = new Button(this);
        b.setText(text);
        b.setOnClickListener(v -> sb.setProgress(
                Math.max(0, Math.min(sb.getMax(), sb.getProgress() + delta))));
        row.addView(b, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }
    }
