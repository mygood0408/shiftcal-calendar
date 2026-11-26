package com.shiftcal.calendar;

/** 2×1 위젯: 오늘 근무 칩 + 오늘 일정 최대 3줄 */
public class ShiftWidget2x1 extends ShiftWidgetBase {
    @Override
    int kind() { return ShiftWidgetRenderer.KIND_2X1; }
}
