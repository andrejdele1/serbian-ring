package com.example.glowring;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.function.IntConsumer;

public class MainActivity extends Activity {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    // Everything a saved look stores, with default values.
    private static final String[] INT_KEYS =
            {"w", "h", "round", "rot", "thick", "spin", "pulse", "px", "py", "tail", "flash"};
    private static final int[] INT_DEFS =
            {30, 12, 100, 0, 3, 30, 0, 300, 14, 50, 3};
    private static final String[] BOOL_KEYS =
            {"rev", "smooth", "comet", "hide_land", "hide_full"};
    private static final boolean[] BOOL_DEFS =
            {false, false, false, true, false};

    private static final String[] FLAG_NAMES =
            {"Serbia", "Russia", "Ukraine", "Germany", "France", "Italy", "Rainbow", "White"};
    private static final String[] FLAG_COLORS = {
            "C6363C,0C4076,FFFFFF",
            "FFFFFF,0039A6,D52B1E",
            "0057B7,FFD700",
            "000000,DD0000,FFCE00",
            "0055A4,FFFFFF,EF4135",
            "009246,FFFFFF,CE2B37",
            "FF0000,FF8000,FFE000,00C000,0060FF,8000C0",
            "FFFFFF"};

    private SharedPreferences sp;
    private float d;
    private boolean dark;
    private int cBg, cCard, cCard2, cText, cSub, cAccent, cTrack;
    private LinearLayout swatchRow, stripeBox;
    private TextView notifStatus;

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

    @Override
    protected void onResume() {
        super.onResume();
        updateNotifStatus();
    }

    // ---------- small helpers ----------

    private int dp(float v) {
        return Math.round(v * d);
    }

    private GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(radiusDp));
        return gd;
    }

    private TextView tv(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(primary ? 0xFFFFFFFF : cText);
        b.setBackground(rounded(primary ? cAccent : cCard2, 14));
        b.setStateListAnimator(null);
        return b;
    }

    private Button smallButton(String label, boolean primary) {
        Button b = button(label, primary);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(18), dp(8), dp(18), dp(8));
        return b;
    }

    private int dialogTheme() {
        return dark ? android.R.style.Theme_Material_Dialog_Alert
                : android.R.style.Theme_Material_Light_Dialog_Alert;
    }

    // ---------- main screen ----------

    private void buildUi() {
        dark = sp.getBoolean("dark", true);
        cBg = dark ? 0xFF0F1115 : 0xFFF3F5F9;
        cCard = dark ? 0xFF1B1E25 : 0xFFFFFFFF;
        cCard2 = dark ? 0xFF2A2F39 : 0xFFE6E9F0;
        cText = dark ? 0xFFF2F4F8 : 0xFF14171C;
        cSub = dark ? 0xFF9AA3B2 : 0xFF5F6877;
        cAccent = dark ? 0xFF4C8DFF : 0xFF0C4076;
        cTrack = dark ? 0xFF3A404C : 0xFFD3D8E0;

        getWindow().setStatusBarColor(cBg);
        getWindow().setNavigationBarColor(cBg);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0
                : (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(cBg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(32));
        sv.addView(root);

        // Header: title + live palette stripe, and the theme switch
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(tv("Camera Halo", 26, cText, true));

        stripeBox = new LinearLayout(this);
        stripeBox.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable sbg = rounded(0x00000000, 3);
        sbg.setStroke(dp(1), cSub);
        stripeBox.setBackground(sbg);
        stripeBox.setPadding(dp(1), dp(1), dp(1), dp(1));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(72), dp(8));
        slp.topMargin = dp(6);
        titles.addView(stripeBox, slp);
        head.addView(titles, new LinearLayout.LayoutParams(0, WRAP, 1f));

        Switch themeSw = new Switch(this);
        themeSw.setText(dark ? "Dark  " : "Light  ");
        themeSw.setTextColor(cSub);
        themeSw.setChecked(dark);
        themeSw.setOnCheckedChangeListener((b, on) -> {
            sp.edit().putBoolean("dark", on).apply();
            buildUi();
        });
        head.addView(themeSw);
        root.addView(head);

        // Start / Stop
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button start = button("Start ring", true);
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
        Button stop = button("Stop ring", false);
        stop.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));
        LinearLayout.LayoutParams a1 = new LinearLayout.LayoutParams(0, WRAP, 1f);
        a1.rightMargin = dp(6);
        LinearLayout.LayoutParams a2 = new LinearLayout.LayoutParams(0, WRAP, 1f);
        a2.leftMargin = dp(6);
        actions.addView(start, a1);
        actions.addView(stop, a2);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(MATCH, WRAP);
        alp.topMargin = dp(18);
        root.addView(actions, alp);

        // Colors
        LinearLayout colors = card(root, "Colors");
        colors.addView(tv("Pick a flag or build your own. Stripes run around the ring in order.",
                13, cSub, false));

        HorizontalScrollView chips = new HorizontalScrollView(this);
        chips.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < FLAG_NAMES.length; i++) {
            final String cols = FLAG_COLORS[i];
            Button chip = smallButton(FLAG_NAMES[i], false);
            chip.setOnClickListener(v -> {
                sp.edit().putString("colors", cols).apply();
                refreshPalette();
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(WRAP, WRAP);
            clp.rightMargin = dp(8);
            chipRow.addView(chip, clp);
        }
        chips.addView(chipRow);
        LinearLayout.LayoutParams chipsLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        chipsLp.topMargin = dp(12);
        colors.addView(chips, chipsLp);

        TextView yours = tv("Your colors (tap one to edit)", 13, cSub, false);
        yours.setPadding(0, dp(16), 0, dp(8));
        colors.addView(yours);

        HorizontalScrollView swScroll = new HorizontalScrollView(this);
        swScroll.setHorizontalScrollBarEnabled(false);
        swatchRow = new LinearLayout(this);
        swatchRow.setOrientation(LinearLayout.HORIZONTAL);
        swScroll.addView(swatchRow);
        colors.addView(swScroll);
        refreshPalette();

        Button add = smallButton("+ Add color", false);
        add.setOnClickListener(v -> addColor());
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        addLp.topMargin = dp(12);
        colors.addView(add, addLp);
        addSwitch(colors, "Blend colors smoothly", "smooth", false);

        // Shape
        LinearLayout shape = card(root, "Shape");
        addSeek(shape, "Width (dp)", "w", 500, 30, 100);
        addSeek(shape, "Height (dp)", "h", 300, 12, 50);
        addSeek(shape, "Corner roundness", "round", 100, 100, 25);
        addSeek(shape, "Rotation (degrees)", "rot", 360, 0, 90);
        addSeek(shape, "Thickness (dp)", "thick", 30, 3, 5);

        // Animation and style
        LinearLayout anim = card(root, "Animation & style");
        addSeek(anim, "Spin speed (0 = still)", "spin", 100, 30, 10);
        addSwitch(anim, "Reverse direction", "rev", false);
        addSeek(anim, "Pulse speed (0 = off)", "pulse", 100, 0, 25);
        addSwitch(anim, "Comet mode (bright head, fading tail)", "comet", false);
        addSeek(anim, "Comet tail length (%)", "tail", 90, 50, 30);

        // Notification flash
        LinearLayout notif = card(root, "Notification flash");
        notifStatus = tv("", 13, cSub, false);
        notif.addView(notifStatus);
        updateNotifStatus();
        Button grant = smallButton("Open notification access settings", false);
        grant.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        gLp.topMargin = dp(10);
        notif.addView(grant, gLp);
        addSeek(notif, "Flash duration (seconds, 0 = off)", "flash", 10, 3, 2);

        // Auto-hide
        LinearLayout hide = card(root, "Auto-hide");
        addSwitch(hide, "Hide in landscape (videos, games)", "hide_land", true);
        addSwitch(hide, "Hide when the status bar is hidden (experimental)", "hide_full", false);

        // Position
        LinearLayout pos = card(root, "Position");
        SeekBar sx = addSeek(pos, "Horizontal (300 = center)", "px", 600, 300, 100);
        SeekBar sy = addSeek(pos, "Vertical (dp from top)", "py", 1000, 14, 200);

        TextView fine = tv("Fine tune, 1 dp per tap", 13, cSub, false);
        fine.setPadding(0, dp(18), 0, dp(6));
        pos.addView(fine);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        nudge(row, "\u25C0", sx, -1);
        nudge(row, "\u25B6", sx, 1);
        nudge(row, "\u25B2", sy, -1);
        nudge(row, "\u25BC", sy, 1);
        pos.addView(row);

        // Saved looks
        LinearLayout looks = card(root, "Saved looks");
        looks.addView(tv("Save everything (colors, shape, speed, position) and load it later.",
                13, cSub, false));
        for (int i = 1; i <= 3; i++) {
            final int n = i;
            LinearLayout lrow = new LinearLayout(this);
            lrow.setOrientation(LinearLayout.HORIZONTAL);
            lrow.setGravity(Gravity.CENTER_VERTICAL);
            lrow.setPadding(0, dp(12), 0, 0);

            String nm = sp.getString("slot" + n + "_name", null);
            TextView label = tv(nm != null ? nm : "Slot " + n + " (empty)", 15, cText, false);
            lrow.addView(label, new LinearLayout.LayoutParams(0, WRAP, 1f));

            Button save = smallButton("Save", false);
            save.setOnClickListener(v -> askSaveName(n));
            Button load = smallButton("Load", true);
            load.setOnClickListener(v -> loadSlot(n));
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(WRAP, WRAP);
            sLp.rightMargin = dp(8);
            lrow.addView(save, sLp);
            lrow.addView(load, new LinearLayout.LayoutParams(WRAP, WRAP));
            looks.addView(lrow);
        }

        setContentView(sv);
    }

    private LinearLayout card(LinearLayout root, String title) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(rounded(cCard, 20));
        c.setPadding(dp(18), dp(16), dp(18), dp(18));
        c.addView(tv(title, 18, cAccent, true));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(16);
        root.addView(c, lp);
        return c;
    }

    private SeekBar addSeek(LinearLayout parent, String label, String key,
                            int max, int def, int step) {
        int cur = Math.min(sp.getInt(key, def), max);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setPadding(0, dp(16), 0, 0);
        TextView name = tv(label, 14, cText, false);
        TextView val = tv(String.valueOf(cur), 14, cAccent, true);
        head.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));
        head.addView(val);
        parent.addView(head);

        int pad = dp(14);
        SeekBar sb = new SeekBar(this);
        sb.setPadding(pad, dp(8), pad, dp(2));
        sb.setMax(max);
        sb.setProgress(cur);
        sb.setProgressTintList(ColorStateList.valueOf(cAccent));
        sb.setThumbTintList(ColorStateList.valueOf(cAccent));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(cTrack));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int value, boolean fromUser) {
                sp.edit().putInt(key, value).apply();
                val.setText(String.valueOf(value));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        parent.addView(sb, new LinearLayout.LayoutParams(MATCH, WRAP));

        parent.addView(new TicksView(this, max, step, pad, cSub),
                new LinearLayout.LayoutParams(MATCH, dp(22)));
        return sb;
    }

    private void addSwitch(LinearLayout parent, String label, String key, boolean def) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextColor(cText);
        s.setTextSize(14);
        s.setChecked(sp.getBoolean(key, def));
        ColorStateList cs = new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{cAccent, cSub});
        s.setThumbTintList(cs);
        s.setTrackTintList(cs);
        s.setOnCheckedChangeListener((b, on) -> sp.edit().putBoolean(key, on).apply());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(14);
        parent.addView(s, lp);
    }

    private void nudge(LinearLayout row, String text, SeekBar sb, int delta) {
        Button b = button(text, false);
        b.setOnClickListener(v -> sb.setProgress(
                Math.max(0, Math.min(sb.getMax(), sb.getProgress() + delta))));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        lp.leftMargin = dp(3);
        lp.rightMargin = dp(3);
        row.addView(b, lp);
    }

    // ---------- colors ----------

    private int[] palette() {
        return OverlayService.parseColors(
                sp.getString("colors", OverlayService.DEFAULT_COLORS));
    }

    private void setPalette(int[] c) {
        sp.edit().putString("colors", OverlayService.joinColors(c)).apply();
        refreshPalette();
    }

    private void refreshPalette() {
        int[] cols = palette();

        if (swatchRow != null) {
            swatchRow.removeAllViews();
            for (int i = 0; i < cols.length; i++) {
                final int idx = i;
                View swatch = new View(this);
                GradientDrawable gd = rounded(cols[i], 12);
                gd.setStroke(dp(2), cSub);
                swatch.setBackground(gd);
                swatch.setOnClickListener(v -> showColorDialog(idx));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(48), dp(48));
                lp.rightMargin = dp(8);
                swatchRow.addView(swatch, lp);
            }
        }

        if (stripeBox != null) {
            stripeBox.removeAllViews();
            for (int c : cols) {
                View seg = new View(this);
                seg.setBackgroundColor(c);
                stripeBox.addView(seg, new LinearLayout.LayoutParams(0, MATCH, 1f));
            }
        }
    }

    private void addColor() {
        int[] c = palette();
        if (c.length >= 8) {
            Toast.makeText(this, "Maximum is 8 colors", Toast.LENGTH_SHORT).show();
            return;
        }
        int[] n = Arrays.copyOf(c, c.length + 1);
        n[c.length] = 0xFFFFFFFF;
        setPalette(n);
        showColorDialog(n.length - 1);
    }

    private void showColorDialog(int index) {
        int[] cols = palette();
        final float[] hsv = new float[3];
        Color.colorToHSV(cols[index], hsv);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(12), dp(22), 0);

        View preview = new View(this);
        box.addView(preview, new LinearLayout.LayoutParams(MATCH, dp(56)));
        TextView hex = tv("", 14, cSub, false);
        hex.setPadding(0, dp(8), 0, 0);
        box.addView(hex);

        Runnable update = () -> {
            int c = Color.HSVToColor(hsv);
            preview.setBackground(rounded(c, 12));
            hex.setText(String.format("#%06X", c & 0xFFFFFF));
        };
        dialogSeek(box, "Hue", 360, Math.round(hsv[0]), v -> {
            hsv[0] = v;
            update.run();
        });
        dialogSeek(box, "Saturation", 100, Math.round(hsv[1] * 100), v -> {
            hsv[1] = v / 100f;
            update.run();
        });
        dialogSeek(box, "Brightness", 100, Math.round(hsv[2] * 100), v -> {
            hsv[2] = v / 100f;
            update.run();
        });
        update.run();

        AlertDialog.Builder b = new AlertDialog.Builder(this, dialogTheme());
        b.setTitle("Color " + (index + 1));
        b.setView(box);
        b.setPositiveButton("Save", (dlg, which) -> {
            int[] c = palette();
   
