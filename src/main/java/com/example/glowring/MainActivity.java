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
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.IntConsumer;

public class MainActivity extends Activity {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    // Everything a saved look stores, with default values.
    private static final String[] INT_KEYS = {"w", "h", "round", "rot", "thick", "spin",
            "pulse", "px", "py", "tail", "flash", "repeat", "vibrance"};
    private static final int[] INT_DEFS = {30, 12, 100, 0, 3, 30, 0, 300, 14, 50, 3, 1, 0};
    private static final String[] BOOL_KEYS = {"rev", "smooth", "comet", "hide_land", "hide_full"};
    private static final boolean[] BOOL_DEFS = {false, false, false, true, false};

    private SharedPreferences sp;
    private float d;
    private boolean dark;
    private int cBg, cCard, cCard2, cText, cSub, cAccent, cTrack;
    private int tab;
    private FrameLayout content;
    private LinearLayout tabBar, swatchRow, stripeBox;
    private Button toggleBtn, notifBtn;
    private TextView notifStatus, flagLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences("ring", MODE_PRIVATE);
        d = getResources().getDisplayMetrics().density;
        tab = sp.getInt("tab", 0);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotifStatus();
        updateToggle();
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

    private int[] themeArray() {
        return new int[]{cBg, cCard, cText, cSub, cAccent, dark ? 1 : 0};
    }

    private int[][] pal() {
        return OverlayService.parsePalette(
                sp.getString("colors", OverlayService.DEFAULT_COLORS));
    }

    private void savePal(int[] c, int[] w) {
        sp.edit()
                .putString("colors", OverlayService.joinPalette(c, w))
                .putString("flag", "Custom")
                .apply();
        refreshPalette();
    }

    // ---------- screen frame: header, content, bottom tabs ----------

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

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(cBg);

        // Header: title and palette stripe, plus the Start/Stop button
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(18), dp(16), dp(18), dp(8));

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
        top.addView(titles, new LinearLayout.LayoutParams(0, WRAP, 1f));

        toggleBtn = smallButton("Start", true);
        toggleBtn.setOnClickListener(v -> toggleRing());
        top.addView(toggleBtn, new LinearLayout.LayoutParams(WRAP, WRAP));
        page.addView(top, new LinearLayout.LayoutParams(MATCH, WRAP));

        content = new FrameLayout(this);
        page.addView(content, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(cCard);
        String[] names = {"Look", "Shape", "Motion", "More"};
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            TextView t = tv(names[i], 14, cSub, false);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(14), 0, dp(14));
            t.setOnClickListener(v -> showTab(idx));
            tabBar.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));
        }
        page.addView(tabBar, new LinearLayout.LayoutParams(MATCH, WRAP));

        setContentView(page);
        showTab(tab);
        updateToggle();
    }

    private void showTab(int t) {
        tab = t;
        sp.edit().putInt("tab", t).apply();
        swatchRow = null;
        flagLabel = null;
        notifStatus = null;
        notifBtn = null;

        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(4), dp(18), dp(24));
        sv.addView(root);

        switch (t) {
            case 0:
                buildLook(root);
                break;
            case 1:
                buildShape(root);
                break;
            case 2:
                buildMotion(root);
                break;
            default:
                buildMore(root);
                break;
        }
        content.removeAllViews();
        content.addView(sv, new FrameLayout.LayoutParams(MATCH, MATCH));

        for (int i = 0; i < tabBar.getChildCount(); i++) {
            TextView tvw = (TextView) tabBar.getChildAt(i);
            boolean sel = i == t;
            tvw.setTextColor(sel ? cAccent : cSub);
            tvw.setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
        refreshPalette();
    }

    private void toggleRing() {
        if (OverlayService.isRunning()) {
            stopService(new Intent(this, OverlayService.class));
        } else {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
                Toast.makeText(this, "Allow 'Display over other apps', then tap Start again",
                        Toast.LENGTH_LONG).show();
                return;
            }
            startForegroundService(new Intent(this, OverlayService.class));
        }
        new Handler(Looper.getMainLooper()).postDelayed(this::updateToggle, 500);
    }

    private void updateToggle() {
        if (toggleBtn == null) return;
        boolean on = OverlayService.isRunning();
        toggleBtn.setText(on ? "Stop" : "Start");
        toggleBtn.setBackground(rounded(on ? 0xFFD64545 : cAccent, 14));
    }

    // ---------- tab: Look ----------

    private void buildLook(LinearLayout root) {
        // Saved looks
        LinearLayout looks = card(root, "Saved looks");
        looks.addView(tv("Tap to load. Long-press to save the current look.", 13, cSub, false));
        LinearLayout lrow = new LinearLayout(this);
        lrow.setOrientation(LinearLayout.HORIZONTAL);
        lrow.setPadding(0, dp(12), 0, 0);
        for (int i = 1; i <= 3; i++) {
            final int n = i;
            String nm = sp.getString("slot" + n + "_name", null);
            Button b = button(nm != null ? nm : "Empty " + n, nm != null);
            b.setTextSize(13);
            b.setSingleLine(true);
            b.setEllipsize(TextUtils.TruncateAt.END);
            b.setOnClickListener(v -> {
                if (sp.getString("slot" + n, null) != null) loadSlot(n);
                else askSaveName(n);
            });
            b.setOnLongClickListener(v -> {
                askSaveName(n);
                return true;
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
            lp.leftMargin = dp(3);
            lp.rightMargin = dp(3);
            lrow.addView(b, lp);
        }
        looks.addView(lrow);

        // Flag and colors
        LinearLayout col = card(root, "Colors");

        LinearLayout frow = new LinearLayout(this);
        frow.setOrientation(LinearLayout.HORIZONTAL);
        frow.setGravity(Gravity.CENTER_VERTICAL);
        flagLabel = tv("", 15, cText, true);
        frow.addView(flagLabel, new LinearLayout.LayoutParams(0, WRAP, 1f));
        Button choose = smallButton("Choose flag", true);
        choose.setOnClickListener(v -> FlagPicker.show(this, themeArray(), entry -> applyFlag(entry)));
        frow.addView(choose);
        LinearLayout.LayoutParams frowLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        frowLp.topMargin = dp(8);
        col.addView(frow, frowLp);

        String rec = sp.getString("recent", "");
        if (!rec.isEmpty()) {
            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout chips = new LinearLayout(this);
            chips.setOrientation(LinearLayout.HORIZONTAL);
            for (String nm : rec.split("\\|")) {
                final String entry = Flags.byName(nm);
                if (entry == null) continue;
                Button chip = smallButton(nm, false);
                chip.setOnClickListener(v -> applyFlag(entry));
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(WRAP, WRAP);
                clp.rightMargin = dp(8);
                chips.addView(chip, clp);
            }
            hs.addView(chips);
            LinearLayout.LayoutParams hsLp = new LinearLayout.LayoutParams(MATCH, WRAP);
            hsLp.topMargin = dp(12);
            col.addView(hs, hsLp);
        }

        TextView yours = tv("Your colors (tap one to edit)", 13, cSub, false);
        yours.setPadding(0, dp(16), 0, dp(8));
        col.addView(yours);

        HorizontalScrollView swScroll = new HorizontalScrollView(this);
        swScroll.setHorizontalScrollBarEnabled(false);
        swatchRow = new LinearLayout(this);
        swatchRow.setOrientation(LinearLayout.HORIZONTAL);
        swScroll.addView(swatchRow);
        col.addView(swScroll);

        Button add = smallButton("+ Add color", false);
        add.setOnClickListener(v -> addColor());
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        addLp.topMargin = dp(12);
        col.addView(add, addLp);

        addSwitch(col, "Blend colors smoothly", "smooth", false);
        addSeek(col, "Repeat pattern (times around the ring)", "repeat", 20, 1, 5);
        addSeek(col, "Vibrance (stronger, brighter colors)", "vibrance", 100, 0, 25);
    }

    private void applyFlag(String entry) {
        String name = Flags.name(entry);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("colors", Flags.spec(entry));
        ed.putInt("repeat", Flags.repeat(entry));
        ed.putString("flag", name);

        ArrayList<String> list = new ArrayList<>();
        list.add(name);
        for (String s : sp.getString("recent", "").split("\\|")) {
            if (!s.isEmpty() && !s.equals(name) && list.size() < 6) list.add(s);
        }
        ed.putString("recent", TextUtils.join("|", list));
        ed.apply();
        showTab(tab);
    }

    // ---------- tab: Shape ----------

    private void buildShape(LinearLayout root) {
        LinearLayout size = card(root, "Size & shape");
        addSeek(size, "Width (dp)", "w", 500, 30, 100);
        addSeek(size, "Height (dp)", "h", 300, 12, 50);
        addSeek(size, "Corner roundness", "round", 100, 100, 25);
        addSeek(size, "Rotation (degrees)", "rot", 360, 0, 90);
        addSeek(size, "Thickness (dp)", "thick", 30, 3, 5);

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
    }

    // ---------- tab: Motion ----------

    private void buildMotion(LinearLayout root) {
        LinearLayout spin = card(root, "Spin");
        addSeek(spin, "Spin speed (0 = still)", "spin", 100, 30, 10);
        addSwitch(spin, "Reverse direction", "rev", false);

        LinearLayout fx = card(root, "Effects");
        addSeek(fx, "Pulse speed (0 = off)", "pulse", 100, 0, 25);
        addSwitch(fx, "Comet mode (bright head, fading tail)", "comet", false);
        addSeek(fx, "Comet tail length (%)", "tail", 90, 50, 30);

        LinearLayout smooth = card(root, "Smoothness");
        smooth.addView(tv("Higher is smoother but uses more battery. Max matches your screen's refresh rate.",
                13, cSub, false));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, 0);
        String[] labels = {"30 fps", "60 fps", "Max"};
        int cur = sp.getInt("fpsmode", 1);
        for (int i = 0; i < 3; i++) {
            final int mode = i;
            Button b = button(labels[i], i == cur);
            b.setOnClickListener(v -> {
                sp.edit().putInt("fpsmode", mode).apply();
                showTab(2);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
            lp.leftMargin = dp(3);
            lp.rightMargin = dp(3);
            row.addView(b, lp);
        }
        smooth.addView(row);
    }

    // ---------- tab: More ----------

    private void buildMore(LinearLayout root) {
        LinearLayout notif = card(root, "Notification flash");
        notifStatus = tv("", 13, cSub, false);
        notif.addView(notifStatus);
        notifBtn = smallButton("Open notification access settings", false);
        notifBtn.setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        nLp.topMargin = dp(10);
        notif.addView(notifBtn, nLp);
        updateNotifStatus();
        addSeek(notif, "Flash duration (seconds, 0 = off)", "flash", 10, 3, 2);

        LinearLayout hide = card(root, "Auto-hide");
        addSwitch(hide, "Hide in landscape (videos, games)", "hide_land", true);
        addSwitch(hide, "Hide when the status bar is hidden (experimental)", "hide_full", false);

        LinearLayout cam = card(root, "Camera hole");
        cam.addView(tv("Re-detects the size and position of your camera hole the next time the ring starts.",
                13, cSub, false));
        Button redetect = smallButton("Re-detect camera hole", false);
        redetect.setOnClickListener(v -> {
            sp.edit().remove("calibrated3").apply();
            Toast.makeText(this, "Stop and start the ring to re-detect", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(WRAP, WRAP);
        rLp.topMargin = dp(10);
        cam.addView(redetect, rLp);

        LinearLayout look = card(root, "Appearance");
        Switch themeSw = new Switch(this);
        themeSw.setText(dark ? "Dark theme  " : "Light theme  ");
        themeSw.setTextColor(cText);
        themeSw.setChecked(dark);
        themeSw.setOnCheckedChangeListener((b, on) -> {
            sp.edit().putBoolean("dark", on).apply();
            buildUi();
        });
        look.addView(themeSw);
    }

    // ---------- building blocks ----------

    private LinearLayout card(LinearLayout root, String title) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(rounded(cCard, 20));
        c.setPadding(dp(18), dp(16), dp(18), dp(18));
        c.addView(tv(title, 18, cAccent, true));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.topMargin = dp(14);
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
        head.addView(name,
