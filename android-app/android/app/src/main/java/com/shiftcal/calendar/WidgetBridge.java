package com.shiftcal.calendar;

import android.content.Context;
import android.webkit.JavascriptInterface;

/** 웹앱이 window.ShiftWidget.save(json)으로 넘긴 스냅샷을 저장하고 위젯을 다시 그린다. */
final class WidgetBridge {
    private final Context appContext;

    WidgetBridge(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @JavascriptInterface
    public void save(String json) {
        appContext.getSharedPreferences(ShiftWidgetRenderer.PREFS, 0)
                .edit()
                .putString(ShiftWidgetRenderer.KEY, json)
                .apply();
        ShiftWidgetRenderer.updateAll(appContext);
    }
}
