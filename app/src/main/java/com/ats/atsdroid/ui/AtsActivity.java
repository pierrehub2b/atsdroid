/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */

package com.ats.atsdroid.ui;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.WindowManager;
import android.widget.TextView;

import com.ats.atsdroid.AtsRunner;
import com.ats.atsdroid.R;
import com.ats.atsdroid.utils.DeviceInfo;

public class AtsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "no sleep");
        wakeLock.acquire();

        KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();

        ((TextView) findViewById(R.id.hostLabel)).setText("ATS driver host : " + DeviceInfo.getInstance().getHostName() + ":" + DeviceInfo.getInstance().getPort());
        ((TextView) findViewById(R.id.systemNameLabel)).setText("System name : " + DeviceInfo.getInstance().getSystemName());
        ((TextView) findViewById(R.id.displaySizeLabel)).setText("Resolution : " + DeviceInfo.getInstance().getResolutionWidth() + " x " + DeviceInfo.getInstance().getResolutionHeight());
        ((TextView) findViewById(R.id.deviceSizeLabel)).setText("Device size : " + DeviceInfo.getInstance().getDeviceWidth() + " x " + DeviceInfo.getInstance().getDeviceHeight());
        ((TextView) findViewById(R.id.modelLabel)).setText("Device name : " + DeviceInfo.getInstance().getManufacturer() + " " + DeviceInfo.getInstance().getModel());
    }
}