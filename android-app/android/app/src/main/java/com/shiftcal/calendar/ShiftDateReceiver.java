package com.shiftcal.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 자정(날짜 바뀜)·시간대·시간 변경 때 위젯을 다시 그려 "오늘"을 맞춘다. */
public class ShiftDateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ShiftWidgetRenderer.updateAll(context);
    }
}
