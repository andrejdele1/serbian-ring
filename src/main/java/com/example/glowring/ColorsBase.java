package com.example.glowring;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.function.IntConsumer;

// Colors, color editor, notification status and saved looks.
abstract class ColorsBase extends UiBase {

    // ---------- colors ----------

    @Override
    void refreshPalette() {
        int[][] p = pal();
        if (flagLabel != null) {
            flagLabel.setText("Flag: " + sp.getString("flag", "Serbia"));
        }
        if (swatchRow != null) {
            swatchRow.removeAllViews();
            for (int i = 0; i < p[0].length; i++) {
                final int idx = i;
                View swatch = new View(this);
                GradientDrawable gd = rounded(p[0][i], 12);
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
            for (int i = 0; i < p[0].length; i++) {
                View seg = new View(this);
                seg.setBackgroundColor(p[0][i]);
                stripeBox.addView(seg, new LinearLayout.LayoutParams(0, MATCH, (float) p[1][i]));
            }
        }
    }

    void addColor() {
        int[][] q = pal();
        if (q[0].length >= 30) {
            Toast.makeText(this, "Maximum is 30 colors", Toast.LENGTH_SHORT).show();
            return;
        }
        int[] c = Arrays.copyOf(q[0], q[0].length + 1);
        int[] w = Arrays.copyOf(q[1], q[1].length + 1);
        c[c.length - 1] = 0xFFFFFFFF;
        w[w.length - 1] = 1;
        savePal(c, w);
        showColorDialog(c.length - 1);
    }

    void showColorDialog(int index) {
        int[][] p = pal();
        if (index >= p[0].length) return;
        final int[] wt = {p[1][index]};
        final float[] hsv = new float[3];
        Color.colorToHSV(p[0][index], hsv);

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
        dialogSeek(box, "Width (share of the ring)", 20, wt[0], v -> wt[0] = Math.max(1, v));
        update.run();

        AlertDialog.Builder b = new AlertDialog.Builder(this, dialogTheme());
        b.setTitle("Color " + (index + 1));
        b.setView(box);
        b.setPositiveButton("Save", (dlg, which) -> {
            int[][] q = pal();
            if (index < q[0].length) {
                q[0][index] = Color.HSVToColor(hsv);
                q[1][index] = wt[0];
                savePal(q[0], q[1]);
            }
        });
        b.setNegativeButton("Cancel", null);
        if (p[0].length > 1) {
            b.setNeutralButton("Remove", (dlg, which) -> {
                int[][] q = pal();
                if (q[0].length > 1 && index < q[0].length) {
                    int[] nc = new int[q[0].length - 1];
                    int[] nw = new int[q[0].length - 1];
                    int j = 0;
                    for (int i = 0; i < q[0].length; i++) {
                        if (i != index) {
                            nc[j] = q[0][i];
                            nw[j] = q[1][i];
                            j++;
                        }
                    }
                    savePal(nc, nw);
                }
            });
        }
        b.show();
    }

    void dialogSeek(LinearLayout box, String label, int max, int cur, IntConsumer cb) {
        TextView t = tv(label, 13, cSub, false);
        t.setPadding(0, dp(12), 0, 0);
        box.addView(t);
        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgress(cur);
        sb.setProgressTintList(ColorStateList.valueOf(cAccent));
        sb.setThumbTintList(ColorStateList.valueOf(cAccent));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(cTrack));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int value, boolean fromUser) {
                cb.accept(value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        box.addView(sb, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    // ---------- notification access ----------

    boolean notifAccess() {
        String s = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(getPackageName());
    }

    void updateNotifStatus() {
        if (notifStatus == null) return;
        boolean on = notifAccess();
        notifStatus.setText(on
                ? "Notification access: ON. The ring flashes when a notification arrives."
                : "Notification access: OFF. Turn it on so the ring can flash on notifications.");
        if (notifBtn != null) notifBtn.setVisibility(on ? View.GONE : View.VISIBLE);
    }

    // ---------- saved looks ----------

    void askSaveName(int n) {
        EditText et = new EditText(this);
        et.setSingleLine(true);
        et.setText(sp.getString("slot" + n + "_name", "Look " + n));
        et.setTextColor(cText);
        et.setSelectAllOnFocus(true);

        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(22), dp(8), dp(22), 0);
        box.addView(et, new LinearLayout.LayoutParams(MATCH, WRAP));

        new AlertDialog.Builder(this, dialogTheme())
                .setTitle("Name this look")
                .setView(box)
                .setPositiveButton("Save", (dlg, which) -> {
                    saveSlot(n, et.getText().toString().trim());
                    buildUi();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void saveSlot(int n, String name) {
        if (name.isEmpty()) name = "Look " + n;
        try {
            JSONObject o = new JSONObject();
            for (int i = 0; i < INT_KEYS.length; i++) {
                o.put(INT_KEYS[i], sp.getInt(INT_KEYS[i], INT_DEFS[i]));
            }
            for (int i = 0; i < BOOL_KEYS.length; i++) {
                o.put(BOOL_KEYS[i], sp.getBoolean(BOOL_KEYS[i], BOOL_DEFS[i]));
            }
            o.put("colors", sp.getString("colors", OverlayService.DEFAULT_COLORS));
            o.put("flag", sp.getString("flag", "Serbia"));
            sp.edit()
                    .putString("slot" + n, o.toString())
                    .putString("slot" + n + "_name", name)
                    .apply();
            Toast.makeText(this, "Saved: " + name, Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(this, "Could not save", Toast.LENGTH_SHORT).show();
        }
    }

    void loadSlot(int n) {
        String js = sp.getString("slot" + n, null);
        if (js == null) {
            Toast.makeText(this, "That slot is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject o = new JSONObject(js);
            SharedPreferences.Editor ed = sp.edit();
            for (String k : INT_KEYS) {
                if (o.has(k)) ed.putInt(k, o.getInt(k));
            }
            for (String k : BOOL_KEYS) {
                if (o.has(k)) ed.putBoolean(k, o.getBoolean(k));
            }
            if (o.has("colors")) ed.putString("colors", o.getString("colors"));
            if (o.has("flag")) ed.putString("flag", o.getString("flag"));
            ed.apply();
            buildUi();
        } catch (JSONException e) {
            Toast.makeText(this, "Could not load", Toast.LENGTH_SHORT).show();
        }
    }
              }
