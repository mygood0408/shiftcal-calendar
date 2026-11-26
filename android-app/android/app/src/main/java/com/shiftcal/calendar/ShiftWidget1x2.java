package com.shiftcal.calendar;

/** 1×2 위젯: 위 = 오늘, 아래 = 내일 */
public class ShiftWidget1x2 extends ShiftWidgetBase {
    @Override
    int kind() { return ShiftWidgetRenderer.KIND_1X2; }
}
