package com.example.glowring;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

// Shared look, settings and building blocks for the screens.
abstract class UiBase extends Activity {

    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    // Everything a saved look stores, with default values.
    static final String[] INT_KEYS = {"w", "h", "round", "rot", "thick", "spin",
            "pulse", "px", "py", "tail", "flash", "repeat", "vibrance"};
    static final int[] INT_DEFS = {30, 12, 100, 0, 3, 30, 0, 300, 14, 50, 3, 1, 0};
    static final String[] BOOL_KEYS = {"rev", "smooth", "comet", "hide_land", "hide_full"};
    static final boolean[] BOOL_DEFS = {false, false, false, true, false};

    SharedPreferences sp;
    float d;
    boolean dark;
    int cBg, cCard, cCard2, cText, cSub, cAccent, cTrack;
    LinearLayout swatchRow, stripeBox;
    Button notifBtn;
    TextView notifStatus, flagLabel;

    abstract void buildUi();

    abstract void refreshPalette();

    // ---------- small helpers ----------

    int dp(float v) {
        return Math.round(v * d);
    }

    GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(radiusDp));
        return gd;
    }

    TextView tv(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    Button button(String label, boolean primary) {
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

    Button smallButton(String label, boolean primary) {
        Button b = button(label, primary);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(18), dp(8), dp(18), dp(8));
        return b;
    }

    int dialogTheme() {
        return dark ? android.R.style.Theme_Material_Dialog_Alert
                : android.R.style.Theme_Material_Light_Dialog_Alert;
    }

    int[] themeArray() {
        return new int[]{cBg, cCard, cText, cSub, cAccent, dark ? 1 : 0};
    }

    int[][] pal() {
        return OverlayService.parsePalette(
                sp.getString("colors", OverlayService.DEFAULT_COLORS));
    }

    void savePal(int[] c, int[] w) {
        sp.edit()
                .putString("colors", OverlayService.joinPalette(c, w))
                .putString("flag", "Custom")
                .apply();
        refreshPalette();
    }

    // ---------- building blocks ----------

    LinearLayout card(LinearLayout root, String title) {
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

    SeekBar addSeek(LinearLayout parent, String label, String key,
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

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        parent.addView(sb, new LinearLayout.LayoutParams(MATCH, WRAP));

        parent.addView(new TicksView(this, max, step, pad, cSub),
                new LinearLayout.LayoutParams(MATCH, dp(22)));
        return sb;
    }

    void addSwitch(LinearLayout parent, String label, String key, boolean def) {
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

    void nudge(LinearLayout row, String text, SeekBar sb, int delta) {
        Button b = button(text, false);
        b.setOnClickListener(v -> sb.setProgress(
                Math.max(0, Math.min(sb.getMax(), sb.getProgress() + delta))));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        lp.leftMargin = dp(3);
        lp.rightMargin = dp(3);
        row.addView(b, lp);
    }

    // The little scale under each slider: tick marks with numbers.
    static class TicksView extends View {
        private final int max, step, pad;
        private final float d;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        TicksView(Context c, int max, int step, int pad, int color) {
            super(c);
            this.max = max;
            this.step = step;
            this.pad = pad;
            this.d = c.getResources().getDisplayMetrics().density;
            p.setColor(color);
            p.setTextSize(10 * d);
            p.setTextAlign(Paint.Align.CENTER);
            p.setStrokeWidth(1.5f * d);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth() - 2f * pad;
            for (int v = 0; v <= max; v += step) {
                float x = pad + w * v / max;
                canvas.drawLine(x, 0, x, 5 * d, p);
                canvas.drawText(String.valueOf(v), x, 18 * d, p);
                int mid = v + step / 2;
                if (step % 2 == 0 && mid < max) {
                    float x2 = pad + w * mid / max;
                    canvas.drawLine(x2, 0, x2, 3 * d, p);
                }
            }
        }
    }
                                    }
