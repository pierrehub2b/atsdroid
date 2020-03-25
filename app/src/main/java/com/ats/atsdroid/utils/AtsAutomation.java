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

package com.ats.atsdroid.utils;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.Configurator;
import android.support.test.uiautomator.UiDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ats.atsdroid.AtsRunner;
import com.ats.atsdroid.AtsRunnerUsb;
import com.ats.atsdroid.element.AbstractAtsElement;
import com.ats.atsdroid.element.AtsResponse;
import com.ats.atsdroid.element.AtsResponseBinary;
import com.ats.atsdroid.element.AtsResponseJSON;
import com.ats.atsdroid.element.AtsRootElement;
import com.ats.atsdroid.server.RequestType;
import com.ats.atsdroid.ui.AtsActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AtsAutomation {

    private static final String HOME = "home";
    private static final String BACK = "back";
    private static final String ENTER = "enter";
    private static final String MENU = "menu";
    private static final String SEARCH = "search";
    private static final String APP = "app";
    private static final String DELETE = "delete";

    private static final int swipeSteps = 10;

    private final Instrumentation instrument = InstrumentationRegistry.getInstrumentation();
    private UiDevice device;
    private final UiAutomation automation = instrument.getUiAutomation();


    public final DeviceInfo deviceInfo = DeviceInfo.getInstance();

    private final Context context = InstrumentationRegistry.getTargetContext();

    private List<ApplicationInfo> applications = new ArrayList<>();
    private AtsRootElement rootElement;
    private CaptureScreenServer screenCapture;

    public Boolean usbMode;

    //-------------------------------------------------------
    private AbstractAtsElement found = null;

    private AtsRunner runner;
    public int port;

    public AtsAutomation(int port, AtsRunner runner, String ipAddress, Boolean usb){
        this.usbMode = usb;
        this.port = port;
        this.runner = runner;
        this.device = UiDevice.getInstance(instrument);

        Configurator.getInstance().setWaitForIdleTimeout(0);

        deviceInfo.initDevice(port, device, ipAddress);

        //-------------------------------------------------------------
        // Bitmap factory default
        // ------------------------------------------------------------
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        //-------------------------------------------------------------

        device.pressHome();
        launchAtsWidget();

        loadApplications();

        deviceSleep();

        sendLogs("ATS_DRIVER_RUNNING");
    }

    public static void sendLogs(String message){
        Bundle b = new Bundle();
        b.putString("atsLogs",  message);
        InstrumentationRegistry.getInstrumentation().sendStatus(0, b);
    }

    private void launchAtsWidget(){
        //executeShell("am start -W com.ats.atsdroid/.ui.AtsActivity");
        final Intent atsIntent = new Intent(context, AtsActivity.class);
        atsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        atsIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        atsIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        atsIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        atsIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(atsIntent);
    }

    public void reloadRoot(){

        AccessibilityNodeInfo rootNode = automation.getRootInActiveWindow();
        while (rootNode == null){
            wait(200);
            rootNode = automation.getRootInActiveWindow();
        }
        rootNode.refresh();

        try {
            rootElement = new AtsRootElement(rootNode);
        }catch (Exception e){
            AtsAutomation.sendLogs("Error on reloadRoot, retrying:" + e.getMessage());
            wait(200);
            reloadRoot();
        }
    }

    public void hideKeyboard() {
        // use application level context to avoid unnecessary leaks.
        deviceButton(BACK);
        wait(500);
    }

    public void enterKeyboard() {
        // use application level context to avoid unnecessary leaks.
        deviceButton(ENTER);
    }

    public AbstractAtsElement getElement(String id){
        found = null;
        getElement(rootElement, id);
        return found;
    }

    private void getElement(AbstractAtsElement parent, String id){
        if(parent.getId().equals(id)){
            found = parent;
        }else{
            for (AbstractAtsElement child : parent.getChildren()) {
                if(found == null) {
                    getElement(child, id);
                }
            }
        }
    }

    public JSONObject getRootObject(){
        return rootElement.getJsonObject();
    }

    public List<ApplicationInfo> getApplications(){
        loadApplications();
        return applications;
    }

    public ApplicationInfo getApplicationInfo(String pkg){
        return getApplicationByPackage(pkg);
    }

    private void loadApplications(){

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager pkgManager = context.getPackageManager();
        for(ResolveInfo info : pkgManager.queryIntentActivities(mainIntent, 0)){

            final ActivityInfo activity = info.activityInfo;
            final String pkg = activity.packageName;
            final String act = activity.name;

            if(pkg != null && act != null){
                final ApplicationInfo app = getApplicationByPackage(pkg);
                if(app == null){
                    String version = "";
                    try {
                        version = pkgManager.getPackageInfo(pkg, 0).versionName;
                    } catch (PackageManager.NameNotFoundException e) {
                        AtsAutomation.sendLogs("Error, cannot get version name:" + e.getMessage());
                    }

                    applications.add(new ApplicationInfo(
                            pkg,
                            act,
                            version,
                            (activity.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                            activity.loadLabel(pkgManager),
                            activity.loadIcon(pkgManager)));
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    private ApplicationInfo getApplicationByPackage(String pkg){
        for (ApplicationInfo app : applications) {
            if (app.packageEquals(pkg)) {
                return app;
            }
        }
        return null;
    }

    public void deleteBackButton(){
        device.pressDelete();
    }

    public void deleteForward(){
        device.pressKeyCode(KeyEvent.KEYCODE_FORWARD_DEL);
    }

    public void deviceButton(String button){
        if(HOME.equals(button)){
            device.pressHome();
        }else if(BACK.equals(button)) {
            device.pressBack();
        }else if(ENTER.equals(button)){
            device.pressEnter();
        }else if(MENU.equals(button)){
            device.pressMenu();
        }else if(SEARCH.equals(button)){
            device.pressSearch();
        }else if(APP.equals(button)){
            try {
                device.pressRecentApps();
            }catch(RemoteException e){}
        }else if(DELETE.equals(button)){
            deleteBackButton();
        }
    }

    private String executeShell(String value){
        try {
            return device.executeShellCommand(value);
        }catch(Exception e){
            AtsAutomation.sendLogs("Error exceute shell command:" + e.getMessage());
        }
        return "";
    }

    public void wait(int ms){
        try {
            Thread.sleep(ms);
        }catch(InterruptedException e){}
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void clickAt(int x, int y){
        device.click(x, y);
        wait(500);
    }

    public void pressNumericKey(int key){
        device.pressKeyCode(key);
        wait(150);
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void swipe(int x, int y, int xTo, int yTo){
        device.swipe(x, y, x + xTo, y + yTo, swipeSteps);
        wait(swipeSteps*6);
    }

    //----------------------------------------------------------------------------------------------------
    // Driver start stop
    //----------------------------------------------------------------------------------------------------

    private boolean driverStarted = false;

    public int getScreenCapturePort(){
        if(screenCapture != null){
            return screenCapture.getPort();
        }
        return -1;
    }

    public void startDriver(){
        if(!driverStarted) {
            driverStarted = true;

            deviceWakeUp();
            executeShell("svc power stayon true");

            launchAtsWidget();

            if(!usbMode) {
                screenCapture = new CaptureScreenServer(this);
                (new Thread(screenCapture)).start();
            }

            //sendLogs("ATS_DRIVER_START:" + user);
        }
    }

    public void stopDriver(){

        if(driverStarted) {
            forceStop("ATS_DRIVER_STOP");
        }
    }

    public void forceStop(String log) {
        driverStarted = false;

        sendLogs(log);

        executeShell("svc power stayon false");
        launchAtsWidget();

        if (screenCapture != null) {
            screenCapture.stop();
            screenCapture = null;
        }

        deviceSleep();
    }

    //----------------------------------------------------------------------------------------------------
    // Driver start stop
    //----------------------------------------------------------------------------------------------------

    private void deviceWakeUp(){
        try {
            device.setOrientationNatural();
            device.freezeRotation();
            device.wakeUp();
        }catch(RemoteException e){}
    }

    private void deviceSleep(){
        try {
            device.unfreezeRotation();
            device.sleep();
        }catch(RemoteException e){}
    }

    public void terminate(){
        runner.stop();
        executeShell("am force-stop com.ats.atsdroid");
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public ApplicationInfo startChannel(String pkg){
        final ApplicationInfo app = getApplicationByPackage(pkg);
        if(app != null) {
            device.pressHome();
            //app.start(context, device);

            executeShell("am start -W -S -f 4194304 -f 268435456 -f 65536 -f 1073741824 -f 2097152 -f 32 -n " + app.getPackageActivityName());
            /*
                4194304 = FLAG_ACTIVITY_BROUGHT_TO_FRONT
                268435456 = FLAG_ACTIVITY_NEW_TASK
                65536 = FLAG_ACTIVITY_NO_ANIMATION
                1073741824 = FLAG_ACTIVITY_NO_HISTORY
                2097152 = FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                32 = FLAG_INCLUDE_STOPPED_PACKAGES

                134217728 = FLAG_ACTIVITY_MULTIPLE_TASK
            */

            reloadRoot();
        }
        return app;
    }

    public String getActivityName(String pkg){
        final ApplicationInfo app = getApplicationByPackage(pkg);
        if(app != null) {
            return app.getPackageActivityName();
        }
        return "";
    }

    public void switchChannel(String pkg){

        AccessibilityNodeInfo rootNode = null;
        while (rootNode == null){
            rootNode = automation.getRootInActiveWindow();
        }

        if(pkg.contentEquals(rootNode.getPackageName())){
            return;
        }

        final ApplicationInfo app = getApplicationByPackage(pkg);
        if(app != null) {
            device.pressHome();
            executeShell("am start -f 536870912 " + app.getPackageActivityName() + "\n");
            reloadRoot();
        }
    }

    public void stopChannel(String pkg){
        stopActivity(pkg);
        launchAtsWidget();
    }

    private void stopActivity(String pkg){
        if(pkg != null){
            executeShell("am force-stop " + pkg + "\n");
        }
    }

    //----------------------------------------------------------------------------------------------------
    // Screen capture
    //----------------------------------------------------------------------------------------------------

    public byte[] getScreenData() {
        return getResizedScreenByteArray(Bitmap.CompressFormat.JPEG, 76);
    }

    public byte[] getScreenDataHires() {
        return getScreenByteArray(Bitmap.CompressFormat.PNG, 100);
    }

    private byte[] getResizedScreenByteArray(Bitmap.CompressFormat cf, int level){
        Bitmap screen = getScreenBitmap();
        screen = Bitmap.createBitmap(screen, 0, 0, deviceInfo.getChannelWidth(), deviceInfo.getChannelHeight(), deviceInfo.getMatrix(), true);
        return getBitmapBytes(screen, cf, level);
    }

    private byte[] getScreenByteArray(Bitmap.CompressFormat cf, int level){
        return getBitmapBytes(getScreenBitmap(), cf, level);
    }

    private Bitmap getScreenBitmap(){
        Bitmap screen = automation.takeScreenshot();
        if (screen == null) {
            screen = createEmptyBitmap(deviceInfo.getChannelWidth(), deviceInfo.getChannelHeight());
        }
        return screen;
    }

    private byte[] getBitmapBytes(Bitmap screen, Bitmap.CompressFormat cf, int level){
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        screen.compress(cf, level, outputStream);
        screen.recycle();

        final byte[] bytes = outputStream.toByteArray();
        try {
            outputStream.close();
        }catch (IOException e){}

        return bytes;
    }

    private Bitmap createEmptyBitmap(int width, int height) {

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setHasAlpha(false);

        final Canvas canvas = new Canvas(bitmap);

        final Paint paint = new Paint();
        paint.setARGB(255, 220, 220, 220);
        canvas.drawRect(0, 0, width, height, paint);

        //canvas.translate(0, -statusBarHeight);

        rootElement.drawElements(canvas, context.getResources());
        return bitmap;
    }

    //--------------------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------

    private final static String EMPTY_DATA = "&empty;";
    private final static String ENTER_KEY = "$KEY-ENTER";
    private final static String TAB_KEY = "$KEY-TAB";

    public AtsResponse executeRequest(RequestType req, Boolean usb) {

        JSONObject obj = new JSONObject();

        try {
            obj.put("type", req.type);

            if (RequestType.APP.equals(req.type)) {
                if (req.parameters.length > 1) {
                    if (RequestType.START.equals(req.parameters[0])) {
                        try {
                            final ApplicationInfo app = startChannel(req.parameters[1]);
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
                        stopChannel(req.parameters[1]);
                        obj.put("status", "0");
                        obj.put("message", "stop app : " + req.parameters[1]);
                    } else if (RequestType.SWITCH.equals(req.parameters[0])) {
                        switchChannel(req.parameters[1]);
                        obj.put("status", "0");
                        obj.put("message", "switch app : " + req.parameters[1]);
                    } else if (RequestType.INFO.equals(req.parameters[0])) {
                        final ApplicationInfo app = getApplicationInfo(req.parameters[1]);
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
                    DeviceInfo.getInstance().driverInfoBase(obj, device.getDisplayHeight());

                    obj.put("status", "0");
                    obj.put("message", "device capabilities");
                    obj.put("id", DeviceInfo.getInstance().getDeviceId());
                    obj.put("model", DeviceInfo.getInstance().getModel());
                    obj.put("manufacturer", DeviceInfo.getInstance().getManufacturer());
                    obj.put("brand", DeviceInfo.getInstance().getBrand());
                    obj.put("version", DeviceInfo.getInstance().getVersion());
                    obj.put("bluetoothName", DeviceInfo.getInstance().getBtAdapter());

                    List<ApplicationInfo> apps = getApplications();

                    JSONArray applications = new JSONArray();
                    for (ApplicationInfo appInfo : apps) {
                        applications.put(appInfo.getJson());
                    }

                    obj.put("applications", applications);

                } catch (Exception e) {
                    AtsAutomation.sendLogs("Error when getting device info:" + e.getMessage());
                    obj.put("status", "-99");
                    obj.put("message", e.getMessage());
                }

            } else if (RequestType.DRIVER.equals(req.type)) {
                if (req.parameters.length > 0) {
                    if (RequestType.START.equals(req.parameters[0])) {

                        startDriver();
                        DeviceInfo.getInstance().driverInfoBase(obj, device.getDisplayHeight());
                        obj.put("status", "0");

                        if(usbMode/* && req.parameters.length > 2*/) {
                            int screenCapturePort = ((AtsRunnerUsb)runner).udpPort;
                            obj.put("screenCapturePort", screenCapturePort);
                            /*if(req.parameters[1].indexOf("true") > -1) {
                                obj.put("udpEndPoint", req.parameters[2]);
                                obj.put("screenCapturePort", screenCapturePort);
                            } else {
                                obj.put("screenCapturePort", req.parameters[3]);
                            }*/
                        } else {
                            obj.put("screenCapturePort", screenCapture.getPort());
                        }
                    } else if (RequestType.STOP.equals(req.parameters[0])) {

                        stopDriver();
                        obj.put("status", "0");
                        obj.put("message", "stop ats driver");

                    } else if (RequestType.QUIT.equals(req.parameters[0])) {

                        stopDriver();
                        obj.put("status", "0");
                        obj.put("message", "close ats driver");

                        terminate();

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
                    deviceButton(req.parameters[0]);
                    obj.put("status", "0");
                    obj.put("message", "button : " + req.parameters[0]);
                } else {
                    obj.put("status", "-31");
                    obj.put("message", "missing button type");
                }

            } else if (RequestType.CAPTURE.equals(req.type)) {

                reloadRoot();
                obj = getRootObject();

            } else if (RequestType.ELEMENT.equals(req.type)) {
                if (req.parameters.length > 2) {
                    AbstractAtsElement element = getElement(req.parameters[0]);

                    if (element != null) {

                        if (RequestType.INPUT.equals(req.parameters[1])) {

                            obj.put("status", "0");

                            String text = req.parameters[2];
                            if (EMPTY_DATA.equals(text)) {
                                obj.put("message", "element clear text");
                                element.clearText(this);
                            } else if(ENTER_KEY.equals(text)) {
                                obj.put("message", "press enter on keyboard");
                                enterKeyboard();
                            } else if(TAB_KEY.equals(text)) {
                                obj.put("message", "hide keyboard");
                                hideKeyboard();
                            } else {
                                element.inputText(this, text);
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
                                    AtsAutomation.sendLogs("Error not enough parameters:" + e.getMessage());
                                }
                            }

                            if (RequestType.TAP.equals(req.parameters[1])) {

                                element.click(this, offsetX, offsetY);

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
                                        AtsAutomation.sendLogs("Error not enough parameters:" + e.getMessage());
                                    }
                                }
                                element.swipe(this, offsetX, offsetY, directionX, directionY);
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
                if(req.parameters.length > 0 && req.parameters[0].indexOf(RequestType.SCREENSHOT_HIRES) == 0){
                    return new AtsResponseBinary(getScreenDataHires());
                }else{
                    return new AtsResponseBinary(getScreenData());
                }
            } else {
                obj.put("status", "-12");
                obj.put("message", "unknown command : " + req.type);
            }

        } catch (JSONException e) {
            sendLogs("Json Error -> " + e.getMessage());
        }

        return new AtsResponseJSON(obj);
    }
}