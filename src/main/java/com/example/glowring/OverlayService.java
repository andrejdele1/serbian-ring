package com.example.glowring;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

public class OverlayService extends Service {

    // Colors are "RRGGBB:width" pairs, in order around the ring.
    static final String DEFAULT_COLORS = "E8202D:1,1E56D8:1,FFFFFF:1";
    static volatile RingView activeRing;

    private WindowManager wm;
    private RingView ring;
    private SharedPreferences sp;

    static boolean isRunning() {
        return activeRing != null;
    }

    // Returns {colors, widths}.
    static int[][] parsePalette(String s) {
        try {
            String[] parts = s.split(",");
            int[] cols = new int[parts.length];
            int[] wts = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                String[] cw = parts[i].trim().split(":");
                cols[i] = 0xFF000000 | (int) Long.parseLong(cw[0].trim(), 16);
                int w = 1;
                if (cw.length > 1) w = Integer.parseInt(cw[1].trim());
                wts[i] = Math.max(1, Math.min(20, w));
            }
            return new int[][]{cols, wts};
        } catch (Exception e) {
            return new int[][]{{0xFFE8202D, 0xFF1E56D8, 0xFFFFFFFF}, {1, 1, 1}};
        }
    }

    static String joinPalette(int[] c, int[] w) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < c.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format("%06X", c[i] & 0xFFFFFF)).append(':').append(w[i]);
        }
        return sb.toString();
    }

    // Boosts saturation and lifts dark colors. White, gray and black are left alone.
    static int vibrance(int color, int k) {
        if (k <= 0) return color;
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        if (hsv[1] < 0.15f || hsv[2] < 0.15f) return color;
        float f = k / 100f;
        hsv[1] = Math.min(1f, hsv[1] * (1f + f));
        hsv[2] = Math.min(1f, hsv[2] + (1f - hsv[2]) * f * 0.6f);
        return Color.HSVToColor(hsv);
    }

    // Called by NotifService when a notification arrives.
    static void trigger(Context c) {
        int secs = c.getSharedPreferences("ring", Context.MODE_PRIVATE).getInt("flash", 3);
        RingView r = activeRing;
        if (secs <= 0 || r == null) return;
        r.flashUntil = SystemClock.uptimeMillis() + secs * 1000L;
        r.postInvalidate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sp = getSharedPreferences("ring", MODE_PRIVATE);

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(
                new NotificationChannel("ring", "Ring", NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, "ring")
                .setContentTitle("Camera Halo is on")
                .setSmallIcon(android.R.drawable.star_on)
                .build();
        startForeground(1, n);

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        ring = new RingView(this);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        if (Build.VERSION.SDK_INT >= 30) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        }
        wm.addView(ring, lp);
        activeRing = ring;

        ring.post(this::autoDetect);
    }

    // Read the camera cutout size and position from the system, once.
    private void autoDetect() {
        if (sp.getBoolean("calibrated3", false)) return;
        WindowInsets wi = ring.getRootWindowInsets();
        if (wi == null) return;
        DisplayCutout dc = wi.getDisplayCutout();
        if (dc == null || dc.getBoundingRects().isEmpty()) return;
        Rect r = dc.getBoundingRects().get(0);
        float d = getResources().getDisplayMetrics().density;
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int px = Math.round((r.centerX() - screenW / 2f) / d) + 300;
        int py = Math.round(r.centerY() / d);
        int w = Math.round(r.width() / d) + 4;
        int h = Math.round(r.height() / d) + 4;
        sp.edit()
                .putInt("px", Math.max(0, Math.min(600, px)))
                .putInt("py", Math.max(0, Math.min(1000, py)))
                .putInt("w", Math.max(2, Math.min(500, w)))
                .putInt("h", Math.max(2, Math.min(300, h)))
                .putBoolean("calibrated3", true)
                .apply();
    }

    @Override
    public void onDestroy() {
        activeRing = null;
        if (ring != null) wm.removeView(ring);
        super.onDestroy();
    }

    // Draws the ring: any colors and widths, spin, pulse, comet, notification flash.
    static class RingView extends View {

        volatile long flashUntil = 0;

        private final SharedPreferences sp;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix m = new Matrix();
        private SweepGradient shader;
        private String shaderKey = "";
        private boolean barsHidden = false;

        private final SharedPreferences.OnSharedPreferenceChangeListener listener =
                (prefs, key) -> invalidate();

        RingView(Context c) {
            super(c);
            sp = c.getSharedPreferences("ring", Context.MODE_PRIVATE);
            p.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            sp.registerOnSharedPreferenceChangeListener(listener);
        }

        @Override
        protected void onDetachedFromWindow() {
            sp.unregisterOnSharedPreferenceChangeListener(listener);
            super.onDetachedFromWindow();
        }

        @Override
        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
            if (Build.VERSION.SDK_INT >= 30) {
                boolean hidden = !insets.isVisible(WindowInsets.Type.statusBars());
                if (hidden != barsHidden) {
                    barsHidden = hidden;
                    invalidate();
                }
            }
            return super.onApplyWindowInsets(insets);
        }

        private static SweepGradient buildShader(int[] c, int[] wts, int repeat,
                                                 boolean smooth, boolean comet,
                                                 float tail, float cx, float cy) {
            int n = c.length;

            if (comet) {
                int[] cols = new int[n + 3];
                float[] pos = new float[n + 3];
                cols[0] = c[n - 1] & 0x00FFFFFF;
                pos[0] = 0f;
                for (int i = 0; i < n; i++) {
                    int a = Math.round(255f * (i + 1) / n);
                    cols[i + 1] = (a << 24) | (c[n - 1 - i] & 0x00FFFFFF);
                    pos[i + 1] = tail * (i + 1) / n;
                }
                cols[n + 1] = c[0] & 0x00FFFFFF;
                pos[n + 1] = tail;
                cols[n + 2] = c[0] & 0x00FFFFFF;
                pos[n + 2] = 1f;
                return new SweepGradient(cx, cy, cols, pos);
            }

            int rep = Math.max(1, Math.min(repeat, 200 / n));
            int total = n * rep;
            float sumW = 0;
            for (int w : wts) sumW += w;
            float totalW = sumW * rep;

            if (smooth) {
                int[] cols = new int[total + 1];
                float[] pos = new float[total + 1];
                float acc = 0;
                int idx = 0;
                for (int r = 0; r < rep; r++) {
                    for (int i = 0; i < n; i++) {
                        cols[idx] = c[i];
                        pos[idx] = acc / totalW;
                        acc += wts[i];
                        idx++;
                    }
                }
                cols[total] = c[0];
                pos[total] = 1f;
                return new SweepGradient(cx, cy, cols, pos);
            }

            int[] cols = new int[2 * total];
            float[] pos = new float[2 * total];
            float acc = 0;
            int idx = 0;
            for (int r = 0; r < rep; r++) {
                for (int i = 0; i < n; i++) {
                    float start = acc / totalW;
                    acc += wts[i];
                    float end = acc / totalW;
                    cols[2 * idx] = c[i];
                    pos[2 * idx] = start;
                    cols[2 * idx + 1] = c[i];
                    pos[2 * idx + 1] = end;
                    idx++;
                }
            }
            pos[2 * total - 1] = 1f;
            return new SweepGradient(cx, cy, cols, pos);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (sp.getBoolean("hide_land", true) && getWidth() > getHeight()) return;
            if (sp.getBoolean("hide_full", false) && barsHidden) return;

            long now = SystemClock.uptimeMillis();
            float d = getResources().getDisplayMetrics().density;
            float w = Math.max(2, sp.getInt("w", 30)) * d;
            float h = Math.max(2, sp.getInt("h", 12)) * d;
            float t = Math.max(1, sp.getInt("thick", 3)) * d;
            float roundPct = sp.getInt("round", 100) / 100f;
            float rot = sp.getInt("rot", 0);
            int speed = sp.getInt("spin", 30);
            int pulse = sp.getInt("pulse", 0);
            boolean rev = sp.getBoolean("rev", false);
            boolean smooth = sp.getBoolean("smooth", false);
            boolean comet = sp.getBoolean("comet", false);
            float tail = Math.max(10, sp.getInt("tail", 50)) / 100f;
            int repeat = Math.max(1, sp.getInt("repeat", 1));
            int vib = sp.getInt("vibrance", 0);
            int fpsMode = sp.getInt("fpsmode", 1);
            String colorStr = sp.getString("colors", DEFAULT_COLORS);

            float cx = getWidth() / 2f + (sp.getInt("px", 300) - 300) * d;
            float cy = sp.getInt("py", 14) * d;

            boolean flashing = now < flashUntil;
            if (flashing) {
                t *= 1.6f;
                speed = Math.max(speed, 20) * 3;
            }

            String key = colorStr + "|" + repeat + "|" + vib + "|" + smooth + "|" + comet
                    + "|" + tail + "|" + cx + "|" + cy;
            if (shader == null || !key.equals(shaderKey)) {
                int[][] pal = parsePalette(colorStr);
                int[] cols = new int[pal[0].length];
                for (int i = 0; i < cols.length; i++) {
                    cols[i] = vibrance(pal[0][i], vib);
                }
                shader = buildShader(cols, pal[1], repeat, smooth, comet, tail, cx, cy);
                shaderKey = key;
            }

            double sec = now / 1000.0;
            float angle = (float) ((sec * speed * 2.0 * (rev ? -1 : 1)) % 360.0);
            m.setRotate(angle, cx, cy);
            shader.setLocalMatrix(m);

            float alpha = 1f;
            if (flashing) {
                alpha = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(2 * Math.PI * 3 * sec));
            } else if (pulse > 0) {
                alpha = 0.4f + 0.6f * (0.5f + 0.5f
                        * (float) Math.sin(2 * Math.PI * (pulse / 25.0) * sec));
            }

            p.setShader(shader);
            p.setAlpha(Math.round(255 * alpha));
            p.setStrokeWidth(t);

            canvas.save();
            canvas.rotate(rot, cx, cy);
            RectF rect = new RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
            float corner = roundPct * Math.min(w, h) / 2f;
            canvas.drawRoundRect(rect, corner, corner, p);
            canvas.restore();

            if (speed > 0 || pulse > 0 || flashing) {
                int rad = (int) (Math.hypot(w, h) / 2 + t + 8 * d);
                int l = (int) cx - rad;
                int tp = (int) cy - rad;
                int rt = (int) cx + rad;
                int bt = (int) cy + rad;
                long target = fpsMode == 0 ? 33 : (fpsMode == 1 ? 16 : 0);
                long delay = target - (SystemClock.uptimeMillis() - now);
                if (delay > 0) {
                    postInvalidateDelayed(delay, l, tp, rt, bt);
                } else {
                    postInvalidateOnAnimation(l, tp, rt, bt);
                }
            }
        }
    }
        }
