package com.example.glowring;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class FlagPicker {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    interface OnPick {
        void picked(String entry);
    }

    // t = {background, card, text, subtext, accent, dark(1/0)}
    static void show(Activity a, int[] t, OnPick cb) {
        final float d = a.getResources().getDisplayMetrics().density;
        final int bg = t[0];
        final int card = t[1];
        final int text = t[2];
        final int sub = t[3];
        final int accent = t[4];
        final boolean dark = t[5] == 1;

        final Dialog dlg = new Dialog(a, dark
                ? android.R.style.Theme_Material_NoActionBar
                : android.R.style.Theme_Material_Light_NoActionBar);

        final ArrayList<String> all = new ArrayList<>();
        Collections.addAll(all, Flags.DATA);
        Collections.sort(all, new Comparator<String>() {
            @Override
            public int compare(String x, String y) {
                return Flags.name(x).compareToIgnoreCase(Flags.name(y));
            }
        });
        final ArrayList<String> shown = new ArrayList<>(all);

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(bg);
        box.setPadding((int) (16 * d), (int) (24 * d), (int) (16 * d), 0);

        LinearLayout head = new LinearLayout(a);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(a);
        title.setText("Choose a flag");
        title.setTextSize(22);
        title.setTextColor(text);
        head.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));
        TextView close = new TextView(a);
        close.setText("Close");
        close.setTextSize(16);
        close.setTextColor(accent);
        close.setPadding((int) (12 * d), (int) (8 * d), (int) (4 * d), (int) (8 * d));
        close.setOnClickListener(v -> dlg.dismiss());
        head.addView(close);
        box.addView(head);

        EditText search = new EditText(a);
        search.setHint("Search flags");
        search.setSingleLine(true);
        search.setTextColor(text);
        search.setHintTextColor(sub);
        search.setPadding((int) (16 * d), (int) (12 * d), (int) (16 * d), (int) (12 * d));
        GradientDrawable sbg = new GradientDrawable();
        sbg.setColor(card);
        sbg.setCornerRadius(14 * d);
        search.setBackground(sbg);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(MATCH, WRAP);
        slp.topMargin = (int) (12 * d);
        slp.bottomMargin = (int) (8 * d);
        box.addView(search, slp);

        ListView list = new ListView(a);
        list.setDivider(null);
        list.setVerticalScrollBarEnabled(false);

        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return shown.size();
            }

            @Override
            public Object getItem(int i) {
                return shown.get(i);
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public View getView(int i, View convertView, ViewGroup parent) {
                String e = shown.get(i);
                LinearLayout row = new LinearLayout(a);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding((int) (6 * d), (int) (12 * d), (int) (6 * d), (int) (12 * d));

                StripView strip = new StripView(a, Flags.spec(e), Flags.repeat(e));
                row.addView(strip, new LinearLayout.LayoutParams((int) (64 * d), (int) (22 * d)));

                TextView name = new TextView(a);
                name.setText(Flags.name(e));
                name.setTextSize(16);
                name.setTextColor(text);
                LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(WRAP, WRAP);
                nlp.leftMargin = (int) (14 * d);
                row.addView(name, nlp);
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, pos, id) -> {
            cb.picked(shown.get(pos));
            dlg.dismiss();
        });
        box.addView(list, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int af) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim().toLowerCase(Locale.ROOT);
                shown.clear();
                for (String e : all) {
                    if (q.isEmpty() || Flags.name(e).toLowerCase(Locale.ROOT).contains(q)) {
                        shown.add(e);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });

        dlg.setContentView(box);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dlg.show();
    }

    // Small preview of a flag's colors, drawn proportionally.
    static class StripView extends View {
        private final int[] cols;
        private final int[] wts;
        private final int repeat;
        private final float d;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        StripView(Context c, String spec, int repeat) {
            super(c);
            int[][] pal = OverlayService.parsePalette(spec);
            cols = pal[0];
            wts = pal[1];
            this.repeat = Math.max(1, repeat);
            d = c.getResources().getDisplayMetrics().density;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float width = getWidth();
            float h = getHeight();
            int total = 0;
            for (int w : wts) total += w;
            total *= repeat;

            Path path = new Path();
            path.addRoundRect(new RectF(0, 0, width, h), 4 * d, 4 * d, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(path);
            p.setStyle(Paint.Style.FILL);
            float x = 0;
            for (int r = 0; r < repeat; r++) {
                for (int i = 0; i < cols.length; i++) {
                    float w = width * wts[i] / total;
                    p.setColor(cols[i]);
                    canvas.drawRect(x, 0, x + w + 0.5f, h, p);
                    x += w;
                }
            }
            canvas.restore();

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(d);
            p.setColor(0x66888888);
            canvas.drawRoundRect(new RectF(0, 0, width, h), 4 * d, 4 * d, p);
        }
    }
              }
