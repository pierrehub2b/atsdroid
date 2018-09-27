package com.ats.atsdroid;

import android.app.Activity;
import android.app.KeyguardManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

public class AtsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ((TextView) findViewById(R.id.hostLabel)).setText("Host name : " + DeviceInfo.getInstance().getHostName());
        ((TextView) findViewById(R.id.systemNameLabel)).setText("System name : " + DeviceInfo.getInstance().getSystemName());
        ((TextView) findViewById(R.id.displaySizeLabel)).setText("Device size : " + DeviceInfo.getInstance().getDisplayWidth() + " x " + DeviceInfo.getInstance().getDisplayHeight());
        ((TextView) findViewById(R.id.modelLabel)).setText("Device name : " + DeviceInfo.getInstance().getManufacturer() + " " + DeviceInfo.getInstance().getModel());
    }
}