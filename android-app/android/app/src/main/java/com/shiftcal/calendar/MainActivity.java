package com.shiftcal.calendar;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onStart() {
        super.onStart();
        // 웹앱이 위젯 데이터를 넘길 수 있도록 window.ShiftWidget 연결
        getBridge().getWebView().addJavascriptInterface(new WidgetBridge(this), "ShiftWidget");
    }
}
