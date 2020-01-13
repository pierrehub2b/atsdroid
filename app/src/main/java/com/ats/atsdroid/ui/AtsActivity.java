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

import com.ats.atsdroid.AtsRunner;
import com.ats.atsdroid.BuildConfig;
import com.ats.atsdroid.element.AbstractAtsElement;
import com.ats.atsdroid.element.AtsResponse;
import com.ats.atsdroid.element.AtsResponseBinary;
import com.ats.atsdroid.element.AtsResponseJSON;
import com.ats.atsdroid.server.RequestType;
import com.ats.atsdroid.utils.ApplicationInfo;
import com.ats.atsdroid.utils.AtsAutomation;
import com.ats.atsdroid.utils.DeviceInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class AtsActivity extends Activity {

    private final static String EMPTY_DATA = "&empty;";

    private static AtsAutomation automation;
    public static AtsView rootView;
    public static float mScreenDensity;
    public static JSONObject obj = new JSONObject();

    public static void setAutomation(AtsAutomation auto) {
        automation = auto;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootView = new AtsView(this);
        setContentView(rootView);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "no sleep");
        wakeLock.acquire();

        KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();
        mScreenDensity = this.getResources().getDisplayMetrics().density;

        /*int resourceId = this.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = this.getResources().getDimensionPixelSize(resourceId);
        }*/
    }

    @Override
    public void dump (String prefix, FileDescriptor fd, PrintWriter writer, String[] args){
        if(args.length > 0){
            String type = args[0];
            String[] parameters = new String[0];

            if(args.length > 1) {
                parameters = Arrays.copyOfRange(args, 1, args.length);
            }

            RequestType req = new RequestType(type, parameters);
            AtsResponse resp = executeRequest(req, true);
            resp.sendDataToUsbPort(writer);
        }else{
            writer.print("nope");
        }
        writer.flush();
    }

    public static AtsResponse executeRequest(RequestType req, Boolean usb) {
        try {
            obj.put("type", req.type);
            if (RequestType.APP.equals(req.type)) {
                if (req.parameters.length > 1) {
                    if (RequestType.START.equals(req.parameters[0])) {
                        try {
                            final ApplicationInfo app = automation.startChannel(req.parameters[1]);
                            if (app != null) {
                                obj.put("status", "0");
                                obj.put("message", "start app : " + app.getPackageName());
                                obj.put("label", app.getLabel());
                                obj.put("icon", app.getIcon());
                                obj.put("version", app.getVersion());
                            } else {
                                obj.put("status", "-51");
                                obj.put("message", "app package not found : " + req.parameters[1]);
                            }
                        } catch (Exception e) {
                            System.err.println("Ats error : " + e.getMessage());
                        }
                    } else if (RequestType.STOP.equals(req.parameters[0])) {
                        automation.stopChannel(req.parameters[1]);
                        obj.put("status", "0");
                        obj.put("message", "stop app : " + req.parameters[1]);
                    } else if (RequestType.SWITCH.equals(req.parameters[0])) {
                        automation.switchChannel(req.parameters[1]);
                        obj.put("status", "0");
                        obj.put("message", "switch app : " + req.parameters[1]);
                    } else if (RequestType.INFO.equals(req.parameters[0])) {
                        final ApplicationInfo app = automation.getApplicationInfo(req.parameters[1]);
                        if (app != null) {
                            obj.put("status", "0");
                            obj.put("info", app.getJson());
                        } else {
                            obj.put("status", "-81");
                            obj.put("message", "app not found : " + req.parameters[1]);
                        }
                    }
                }
            } else if (RequestType.INFO.equals(req.type)) {
                try {
                    driverInfoBase(obj);

                    obj.put("status", "0");
                    obj.put("message", "device capabilities");
                    obj.put("id", DeviceInfo.getInstance().getDeviceId());
                    obj.put("model", DeviceInfo.getInstance().getModel());
                    obj.put("manufacturer", DeviceInfo.getInstance().getManufacturer());
                    obj.put("brand", DeviceInfo.getInstance().getBrand());
                    obj.put("version", DeviceInfo.getInstance().getVersion());
                    obj.put("bluetoothName", DeviceInfo.getInstance().getBtAdapter());

                    final List<ApplicationInfo> apps = automation.getApplications();

                    JSONArray applications = new JSONArray();
                    for (ApplicationInfo appInfo : apps) {
                        applications.put(appInfo.getJson());
                    }
                    obj.put("applications", applications);

                } catch (Exception e) {
                    obj.put("status", "-99");
                    obj.put("message", e.getMessage());
                }

            } else if (RequestType.DRIVER.equals(req.type)) {
                if (req.parameters.length > 0) {
                    if (RequestType.START.equals(req.parameters[0])) {

                        automation.startDriverThread();

                        driverInfoBase(obj);
                        obj.put("status", "0");
                        if(req.parameters.length > 0) {
                            obj.put("screenCapturePort", req.parameters[1]);
                        } else {
                            obj.put("screenCapturePort", automation.getScreenCapturePort());
                        }
                    } else if (RequestType.STOP.equals(req.parameters[0])) {

                        automation.stopDriverThread();
                        obj.put("status", "0");
                        obj.put("message", "stop ats driver");

                    } else if (RequestType.QUIT.equals(req.parameters[0])) {

                        automation.stopDriverThread();
                        obj.put("status", "0");
                        obj.put("message", "close ats driver");

                        automation.runner.setRunning(false);
                        automation.terminate();
                        return new AtsResponseJSON(obj);
                    } else {
                        obj.put("status", "-42");
                        obj.put("message", "wrong driver action type : " + req.parameters[0]);
                    }
                } else {
                    obj.put("status", "-41");
                    obj.put("message", "missing driver action");
                }

            } else if (RequestType.BUTTON.equals(req.type)) {

                if (req.parameters.length > 0) {
                    automation.deviceButton(req.parameters[0]);
                    obj.put("status", "0");
                    obj.put("message", "button : " + req.parameters[0]);
                } else {
                    obj.put("status", "-31");
                    obj.put("message", "missing button type");
                }

            } else if (RequestType.CAPTURE.equals(req.type)) {

                automation.reloadRoot();
                obj = automation.getRootObject();

            } else if (RequestType.ELEMENT.equals(req.type)) {

                if (req.parameters.length > 2) {
                    AbstractAtsElement element = automation.getElement(req.parameters[0]);
                    if (element != null) {
                        if (RequestType.INPUT.equals(req.parameters[1])) {

                            obj.put("status", "0");

                            String text = req.parameters[2];
                            if (EMPTY_DATA.equals(text)) {
                                obj.put("message", "element clear text");
                                element.clearText(automation);
                            } else {
                                element.inputText(automation, text);
                                obj.put("message", "element send keys : " + text);
                            }
                        } else {

                            int offsetX = 0;
                            int offsetY = 0;

                            if (req.parameters.length > 3) {
                                try {
                                    offsetX = Integer.parseInt(req.parameters[2]);
                                    offsetY = Integer.parseInt(req.parameters[3]);
                                } catch (NumberFormatException e) {
                                }
                            }

                            if (RequestType.TAP.equals(req.parameters[1])) {

                                element.click(automation, offsetX, offsetY);

                                obj.put("status", "0");
                                obj.put("message", "click on element");

                            } else if (RequestType.SWIPE.equals(req.parameters[1])) {
                                int directionX = 0;
                                int directionY = 0;
                                if (req.parameters.length > 5) {
                                    try {
                                        directionX = Integer.parseInt(req.parameters[4]);
                                        directionY = Integer.parseInt(req.parameters[5]);
                                    } catch (NumberFormatException e) {
                                    }
                                }
                                element.swipe(automation, offsetX, offsetY, directionX, directionY);
                                obj.put("status", "0");
                                obj.put("message", "swipe element to " + directionX + ":" + directionY);
                            }
                        }
                    } else {
                        obj.put("status", "-22");
                        obj.put("message", "element not found");
                    }
                } else {
                    obj.put("status", "-21");
                    obj.put("message", "missing element id");
                }
            } else if (RequestType.SCREENSHOT.equals(req.type)) {
                boolean lostLess = req.parameters[req.parameters.length-1].indexOf("True") > -1;
                return new AtsResponseBinary(automation.getScreenDataHires(lostLess));
            } else {
                obj.put("status", "-12");
                obj.put("message", "unknown command : " + req.type);
            }
            return new AtsResponseJSON(obj);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new AtsResponseJSON(obj);
    }

    private static void driverInfoBase(JSONObject obj) throws JSONException {
        obj.put("os", "android");
        obj.put("driverVersion", BuildConfig.VERSION_NAME);
        obj.put("systemName", DeviceInfo.getInstance().getSystemName());
        obj.put("deviceWidth", DeviceInfo.getInstance().getDeviceWidth());
        obj.put("deviceHeight", DeviceInfo.getInstance().getDeviceHeight());
        obj.put("channelWidth", automation.getChannelWidth());
        obj.put("channelHeight", automation.getChannelHeight());
    }

}