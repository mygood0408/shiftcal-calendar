package com.shiftcal.calendar;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 위젯 한 장을 Bitmap으로 그려서 RemoteViews에 넣는다.
 * 데이터는 웹앱(index.html)이 ShiftWidget.save()로 넘긴 스냅샷(JSON)을 쓴다.
 * 색·글자 규칙: 주/야/비/휴 = 코드 색·이름 첫 글자, 지원 = 주간 색, 휴가 = 휴무 색.
 */
final class ShiftWidgetRenderer {
    static final String PREFS = "shiftcal_widget";
    static final String KEY = "snapshot";

    static final int KIND_2X1 = 0, KIND_4X1 = 1, KIND_1X2 = 2;

    private static final int WHITE = Color.WHITE;
    private static final int BLACK = Color.BLACK;
    private static final int GRAY = 0xFF9A938A;
    private static final int MUTED = 0xFF566676;
    private static final String[] WEEK = {"일", "월", "화", "수", "목", "금", "토"};

    private ShiftWidgetRenderer() {}

    static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        updateClass(ctx, mgr, ShiftWidget2x1.class, KIND_2X1);
        updateClass(ctx, mgr, ShiftWidget4x1.class, KIND_4X1);
        updateClass(ctx, mgr, ShiftWidget1x2.class, KIND_1X2);
    }

    static void updateClass(Context ctx, AppWidgetManager mgr, Class<?> cls, int kind) {
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, cls));
        for (int id : ids) updateOne(ctx, mgr, id, kind);
    }

    static void updateOne(Context ctx, AppWidgetManager mgr, int id, int kind) {
        Bundle opt = mgr.getAppWidgetOptions(id);
        int wDp = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int hDp = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        if (wDp <= 0) wDp = defaultW(kind);
        if (hDp <= 0) hDp = defaultH(kind);

        float d = ctx.getResources().getDisplayMetrics().density;
        Bitmap bmp = Bitmap.createBitmap(Math.round(wDp * d), Math.round(hDp * d), Bitmap.Config.ARGB_8888);
        Draw g = new Draw(new Canvas(bmp), d);
        Snap s = Snap.load(ctx);
        if (kind == KIND_2X1) draw2x1(g, s, wDp, hDp);
        else if (kind == KIND_4X1) draw4x1(g, s, wDp, hDp);
        else draw1x2(g, s, wDp, hDp);

        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_frame);
        rv.setImageViewBitmap(R.id.widget_image, bmp);
        PendingIntent pi = PendingIntent.getActivity(ctx, id, new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_image, pi);
        mgr.updateAppWidget(id, rv);
    }

    private static int defaultW(int kind) { return kind == KIND_2X1 ? 250 : kind == KIND_4X1 ? 290 : 120; }
    private static int defaultH(int kind) { return kind == KIND_1X2 ? 250 : 110; }

    // ---------- 2×1: 날짜·반 / 칩 + 오늘 일정 최대 3줄 ----------
    private static void draw2x1(Draw g, Snap s, int w, int h) {
        g.rect(0, 0, w, h, 20, WHITE);
        Calendar now = dayAt(0);
        Info t = info(s, key(now));

        String dateLine = (now.get(Calendar.MONTH) + 1) + "월 " + now.get(Calendar.DAY_OF_MONTH) + "일 (" + koWeek(now) + ")";
        g.text(dateLine, 14, 22, 12, 0xB3000000, true, Paint.Align.LEFT);
        g.text(s.label, w - 14, 22, 12, BLACK, true, Paint.Align.RIGHT);

        float chip = Math.min(58, h - 30 - 14);
        g.rect(14, 30, 14 + chip, 30 + chip, 14, t.color);
        boolean two = t.shortText.length() > 1;
        float fs = two ? 18 : 24;
        g.text(t.shortText, 14 + chip / 2, 30 + chip / 2 + fs * 0.35f, fs, WHITE, true, Paint.Align.CENTER);

        float lx = 14 + chip + 12, maxW = w - lx - 14;
        if (t.items.isEmpty()) {
            g.text("일정 없음", lx, 44, 13, MUTED, false, Paint.Align.LEFT);
        } else {
            for (int i = 0; i < Math.min(3, t.items.size()); i++) {
                g.text(g.fit("· " + t.items.get(i), maxW, 13, false), lx, 44 + i * 19, 13, BLACK, false, Paint.Align.LEFT);
            }
        }
    }

    // ---------- 4×1: 이번 주 날짜 범위 / 7칸 (날짜 위, 칸은 배경과 같은 흰색, 테두리·글자는 근무 색) ----------
    private static void draw4x1(Draw g, Snap s, int w, int h) {
        g.rect(0, 0, w, h, 20, WHITE);
        Calendar today = dayAt(0);
        int back = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7; // 월=0 … 일=6
        Calendar mon = dayAt(-back);
        Calendar sun = dayAt(-back + 6);

        String head = "이번 주 ";
        g.text(head, 12, 20, 12, BLACK, true, Paint.Align.LEFT);
        g.text(md(mon) + " ~ " + md(sun), 12 + g.width(head, 12, true), 20, 12, 0xB3000000, false, Paint.Align.LEFT);
        g.text(s.label, w - 12, 20, 12, BLACK, true, Paint.Align.RIGHT);

        float left = 12, gap = 5, cw = (w - left * 2 - gap * 6) / 7f;
        float dateTop = 28, cellTop = 54, cellBot = h - 12;
        String todayKey = key(today);
        for (int i = 0; i < 7; i++) {
            Calendar c = dayAt(-back + i);
            Info f = info(s, key(c));
            float x0 = left + i * (cw + gap), cx = x0 + cw / 2;
            String dn = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
            if (key(c).equals(todayKey)) {
                g.rect(cx - 11, dateTop + 2, cx + 11, dateTop + 18, 8, BLACK);
                g.text(dn, cx, dateTop + 14, 11, WHITE, true, Paint.Align.CENTER);
            } else {
                g.text(dn, cx, dateTop + 14, 11, MUTED, true, Paint.Align.CENTER);
            }
            g.rect(x0, cellTop, x0 + cw, cellBot, 10, WHITE);
            g.border(x0, cellTop, x0 + cw, cellBot, 10, 2, f.color);
            float fs = f.shortText.length() > 1 ? 11 : 15;
            g.text(f.shortText, cx, (cellTop + cellBot) / 2 + fs * 0.35f, fs, f.color, true, Paint.Align.CENTER);
        }
    }

    // ---------- 1×2: 위 = 오늘, 아래 = 내일 (같은 크기·같은 디자인, 테두리·글자는 근무 색) ----------
    private static void draw1x2(Draw g, Snap s, int w, int h) {
        g.rect(0, 0, w, h, 20, WHITE);
        float pad = 10, gap = 10;
        float boxH = (h - pad * 2 - gap) / 2f;
        for (int i = 0; i < 2; i++) {
            Calendar c = dayAt(i);
            Info f = info(s, key(c));
            float t = pad + i * (boxH + gap), b = t + boxH, l = pad, r = w - pad;
            g.rect(l, t, r, b, 18, WHITE);
            g.border(l, t, r, b, 18, 2, f.color);

            float x = l + 12, maxW = r - x - 10;
            float y = t + 25;
            String date = String.format(Locale.US, "%d/%d %s", c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), koWeek(c));
            g.text(date, x, y, 13, MUTED, true, Paint.Align.LEFT);
            y += 21;
            g.text(g.fit(f.longText, maxW, 17, true), x, y, 17, f.color, true, Paint.Align.LEFT);
            for (String item : f.items) {
                y += 15;
                if (y > b - 8) break;
                g.text(g.fit("· " + item, maxW, 11, false), x, y, 11, BLACK, false, Paint.Align.LEFT);
            }
        }
    }

    // ---------- 데이터 ----------
    static final class Snap {
        String label = "";
        JSONObject days = new JSONObject();
        final Map<String, String> names = new HashMap<>();
        final Map<String, Integer> colors = new HashMap<>();

        static Snap load(Context ctx) {
            Snap s = new Snap();
            String json = ctx.getSharedPreferences(PREFS, 0).getString(KEY, null);
            if (json == null) return s;
            try {
                JSONObject root = new JSONObject(json);
                s.label = root.optString("label", "");
                JSONArray codes = root.optJSONArray("codes");
                if (codes != null) {
                    for (int i = 0; i < codes.length(); i++) {
                        JSONObject c = codes.getJSONObject(i);
                        s.names.put(c.getString("id"), c.getString("name"));
                        s.colors.put(c.getString("id"), Color.parseColor(c.getString("color")));
                    }
                }
                JSONObject d = root.optJSONObject("days");
                if (d != null) s.days = d;
            } catch (Exception e) {
                // 깨진 스냅샷이면 빈 위젯으로 그린다
            }
            return s;
        }

        int color(String id, int fallback) {
            Integer c = colors.get(id);
            return c == null ? fallback : c;
        }
    }

    static final class Info {
        String shortText = "-", longText = "-";
        int color = GRAY;
        final List<String> items = new ArrayList<>();
    }

    static Info info(Snap s, String key) {
        Info f = new Info();
        JSONObject e = s.days.optJSONObject(key);
        if (e == null) return f;

        String code = e.isNull("code") ? null : e.optString("code", null);
        boolean support = e.optBoolean("support", false);
        boolean leave = e.optBoolean("leave", false);

        if (leave) {
            f.shortText = "휴가";
            f.longText = "휴가";
            f.color = s.color("c3", GRAY);
        } else if (code != null) {
            if (support) {
                f.shortText = "지원";
                f.longText = "지원";
                f.color = s.color("c0", GRAY);
            } else {
                String name = s.names.containsKey(code) ? s.names.get(code) : "";
                if (!name.isEmpty()) {
                    f.shortText = name.substring(0, 1);
                    f.longText = name;
                }
                f.color = s.color(code, GRAY);
            }
        }

        if (!e.isNull("holiday")) {
            String hol = e.optString("holiday", "");
            if (!hol.isEmpty()) f.items.add(hol);
        }
        String memo = e.optString("memo", "");
        for (String line : memo.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty()) f.items.add(t);
        }
        return f;
    }

    static String key(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    static Calendar dayAt(int offset) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, offset);
        return c;
    }

    static String koWeek(Calendar c) {
        return WEEK[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    static String md(Calendar c) {
        return String.format(Locale.US, "%02d.%02d", c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    // ---------- 그리기 도우미 (좌표·크기는 dp 단위) ----------
    static final class Draw {
        final Canvas c;
        final float d;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Draw(Canvas c, float d) {
            this.c = c;
            this.d = d;
        }

        private float px(float dp) { return dp * d; }

        void rect(float l, float t, float r, float b, float rad, int fill) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(fill);
            c.drawRoundRect(new RectF(px(l), px(t), px(r), px(b)), px(rad), px(rad), p);
        }

        void border(float l, float t, float r, float b, float rad, float strokeDp, int color) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(px(strokeDp));
            p.setColor(color);
            float in = strokeDp / 2f;
            c.drawRoundRect(new RectF(px(l + in), px(t + in), px(r - in), px(b - in)), px(rad), px(rad), p);
        }

        private void setFont(float sizeDp, boolean bold) {
            p.setTextSize(px(sizeDp));
            p.setTypeface(bold ? Typeface.create(Typeface.DEFAULT, Typeface.BOLD) : Typeface.DEFAULT);
        }

        void text(String s, float xDp, float baseDp, float sizeDp, int color, boolean bold, Paint.Align align) {
            setFont(sizeDp, bold);
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            p.setTextAlign(align);
            c.drawText(s, px(xDp), px(baseDp), p);
        }

        float width(String s, float sizeDp, boolean bold) {
            setFont(sizeDp, bold);
            return p.measureText(s) / d;
        }

        String fit(String s, float maxDp, float sizeDp, boolean bold) {
            if (width(s, sizeDp, bold) <= maxDp) return s;
            String t = s;
            while (t.length() > 1 && width(t + "…", sizeDp, bold) > maxDp) t = t.substring(0, t.length() - 1);
            return t + "…";
        }
    }
}
