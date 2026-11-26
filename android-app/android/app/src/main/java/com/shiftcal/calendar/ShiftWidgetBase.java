package com.shiftcal.calendar;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

/** 위젯 3종(2×1, 4×1, 1×2)의 공통 부분. 각 하위 클래스가 크기 종류만 알려 준다. */
abstract class ShiftWidgetBase extends AppWidgetProvider {
    abstract int kind();

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) ShiftWidgetRenderer.updateOne(context, manager, id, kind());
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int appWidgetId, Bundle newOptions) {
        ShiftWidgetRenderer.updateOne(context, manager, appWidgetId, kind());
    }
}
