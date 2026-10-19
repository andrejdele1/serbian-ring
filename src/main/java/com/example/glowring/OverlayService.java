package com.example.glowring;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Build;
import android.os.IBinder;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

public class OverlayService extends Service {

    private WindowManager wm;
    private RingView ring;
    private SharedPreferences sp;

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
                .setContentTitle("Serbian ring is on")
                .setSmallIcon(android.R.drawable.star_on)
                .build();
        startForeground(1, n);

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        ring = new RingView(this, false);

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
        if (ring != null) wm.removeView(ring);
        super.onDestroy();
    }

    // Draws the spinning red/blue/white ring. Also used for the preview in the app.
    static class RingView extends View {
        static final int RED = 0xFFC6363C;
        static final int BLUE = 0xFF0C4076;
        static final int WHITE = 0xFFFFFFFF;

        private final SharedPreferences sp;
        private final boolean preview;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix m = new Matrix();
        private SweepGradient shader;
        private float lastCx = -1, lastCy = -1;

        private final SharedPreferences.OnSharedPreferenceChangeListener listener =
                (prefs, key) -> invalidate();

        RingView(Context c, boolean preview) {
            super(c);
            this.preview = preview;
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
        protected void onDraw(Canvas canvas) {
            float d = getResources().getDisplayMetrics().density;
            float w = Math.max(2, sp.getInt("w", 30)) * d;
            float h = Math.max(2, sp.getInt("h", 12)) * d;
            float t = Math.max(1, sp.getInt("thick", 3)) * d;
            float roundPct = sp.getInt("round", 100) / 100f;
            float rot = sp.getInt("rot", 0);
            int speed = sp.getInt("spin", 30);

            float baseW = preview
                    ? getResources().getDisplayMetrics().widthPixels
                    : getWidth();
            float cx = baseW / 2f + (sp.getInt("px", 300) - 300) * d;
            float cy = sp.getInt("py", 14) * d;

            if (preview) {
                canvas.drawColor(0xFF1E1E1E);
                float s = getWidth() / baseW;
                canvas.scale(s, s);
            }

            if (shader == null || cx != lastCx || cy != lastCy) {
                shader = new SweepGradient(cx, cy,
                        new int[]{RED, RED, BLUE, BLUE, WHITE, WHITE},
                        new float[]{0f, 1f / 3f, 1f / 3f, 2f / 3f, 2f / 3f, 1f});
                lastCx = cx;
                lastCy = cy;
            }

            double sec = System.nanoTime() / 1e9;
            float angle = (float) ((sec * speed * 2.0) % 360.0);
            m.setRotate(angle, cx, cy);
            shader.setLocalMatrix(m);

            p.setShader(shader);
            p.setStrokeWidth(t);

            canvas.save();
            canvas.rotate(rot, cx, cy);
            RectF rect = new RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
            float corner = roundPct * Math.min(w, h) / 2f;
            canvas.drawRoundRect(rect, corner, corner, p);
            canvas.restore();

            if (speed > 0) {
                if (preview) {
                    postInvalidateOnAnimation();
                } else {
                    int r = (int) (Math.hypot(w, h) / 2 + t + 8 * d);
                    postInvalidateOnAnimation((int) cx - r, (int) cy - r,
                            (int) cx + r, (int) cy + r);
                }
            }
        }
    }
}
