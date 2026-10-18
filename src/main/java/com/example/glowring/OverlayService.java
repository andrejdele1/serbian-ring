package com.example.glowring;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

public class OverlayService extends Service
        implements SharedPreferences.OnSharedPreferenceChangeListener {

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

        sp.registerOnSharedPreferenceChangeListener(this);
        ring.post(this::autoDetect);
    }

    // Try to read the camera hole position from the system, once.
    private void autoDetect() {
        if (sp.getBoolean("calibrated", false)) return;
        WindowInsets wi = ring.getRootWindowInsets();
        if (wi == null) return;
        DisplayCutout dc = wi.getDisplayCutout();
        if (dc == null || dc.getBoundingRects().isEmpty()) return;
        Rect r = dc.getBoundingRects().get(0);
        float d = getResources().getDisplayMetrics().density;
        int w = getResources().getDisplayMetrics().widthPixels;
        int x = Math.round((r.centerX() - w / 2f) / d) + 50;
        int y = Math.round(r.centerY() / d);
        int radius = Math.round(r.width() / 2f / d) + 2;
        sp.edit()
                .putInt("x", Math.max(0, Math.min(100, x)))
                .putInt("y", Math.max(0, Math.min(60, y)))
                .putInt("radius", Math.max(1, Math.min(40, radius)))
                .putBoolean("calibrated", true)
                .apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (ring != null) ring.invalidate();
    }

    @Override
    public void onDestroy() {
        sp.unregisterOnSharedPreferenceChangeListener(this);
        if (ring != null) wm.removeView(ring);
        super.onDestroy();
    }

    // Draws the ring: red (top), blue (middle), white (bottom).
    static class RingView extends View {
        // Edit these three colors to change the flag/ring colors.
        static final int[] COLORS = {0xFFC6363C, 0xFF0C4076, 0xFFFFFFFF};

        private final SharedPreferences sp;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        RingView(Context c) {
            super(c);
            sp = c.getSharedPreferences("ring", Context.MODE_PRIVATE);
            p.setStyle(Paint.Style.STROKE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float d = getResources().getDisplayMetrics().density;
            float r = Math.max(1, sp.getInt("radius", 9)) * d;
            float t = Math.max(1, sp.getInt("thick", 3)) * d;
            float cx = getWidth() / 2f + (sp.getInt("x", 50) - 50) * d;
            float cy = sp.getInt("y", 14) * d;

            p.setStrokeWidth(t);
            float top = cy - r - t / 2f;
            float h = (2 * r + t) / 3f;

            for (int i = 0; i < 3; i++) {
                canvas.save();
                canvas.clipRect(0, top + i * h, getWidth(), top + (i + 1) * h);
                p.setColor(COLORS[i]);
                canvas.drawCircle(cx, cy, r, p);
                canvas.restore();
            }
        }
    }
        }
