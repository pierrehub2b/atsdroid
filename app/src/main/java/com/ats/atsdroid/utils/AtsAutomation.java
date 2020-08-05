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
import android.graphics.*;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.*;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.ats.atsdroid.AtsRunner;
import com.ats.atsdroid.AtsRunnerUsb;
import com.ats.atsdroid.element.*;
import com.ats.atsdroid.response.AtsResponse;
import com.ats.atsdroid.response.AtsResponseBinary;
import com.ats.atsdroid.response.AtsResponseJSON;
import com.ats.atsdroid.scripting.ScriptingExecutor;
import com.ats.atsdroid.server.RequestType;
import com.ats.atsdroid.ui.AtsActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private final UiDevice device;
    private final UiAutomation automation = instrument.getUiAutomation();
    
    public final DeviceInfo deviceInfo = DeviceInfo.getInstance();

    private final Context context = InstrumentationRegistry.getTargetContext();

    private final List<ApplicationInfo> applications = new ArrayList<>();
    private AtsRootElement rootElement;
    private CaptureScreenServer screenCapture;

    public Boolean usbMode;
    private int activeChannelsCount = 0;

    private AbstractAtsElement found = null;

    private final AtsRunner runner;
    public int port;

    public AtsAutomation(int port, AtsRunner runner, String ipAddress, Boolean usb){
        this.usbMode = usb;
        this.port = port;
        this.runner = runner;
        this.device = UiDevice.getInstance(instrument);

        // MotionEvent.obtain()
        // automation.injectInputEvent()
        
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

        sendLogs("ATS_DRIVER_RUNNING\n");
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
            AtsAutomation.sendLogs("root node is null\n");
        }
        rootNode.refresh();

        try {
            rootElement = new AtsRootElement(rootNode);
        } catch (Exception e) {
            AtsAutomation.sendLogs("Error on reloadRoot, retrying:" + e.getMessage() + "\n");
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

    public AbstractAtsElement getElement(String id) {
        found = null;
        getElement(rootElement, id);
        return found;
    }

    private void getElement(AbstractAtsElement parent, String id) {
        if (parent == null) { return; }

        if (parent.getId().equals(id)) {
            found = parent;
        } else {
            for (AbstractAtsElement child : parent.getChildren()) {
                if (found == null) {
                    getElement(child, id);
                }
            }
        }
    }

    public JSONObject getRootObject() {
        return rootElement.getJsonObject();
    }

    public List<ApplicationInfo> getApplications() {
        loadApplications();
        return applications;
    }

    public ApplicationInfo getApplicationInfo(String pkg){
        return getApplicationByPackage(pkg);
    }

    private void loadApplications() {

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
                        AtsAutomation.sendLogs("Error, cannot get version name:" + e.getMessage() + "\n");
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

    private ApplicationInfo getApplicationByPackage(String pkg) {
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
            }catch(RemoteException ignored){}
        }else if(DELETE.equals(button)){
            deleteBackButton();
        }
    }

    private void executeShell(String value) {
        try {
            device.executeShellCommand(value);
        }catch(Exception e){
            AtsAutomation.sendLogs("Error execute shell command:" + e.getMessage() + "\n");
        }
    }

    public void wait(int ms) {
        try {
            Thread.sleep(ms);
        }catch(InterruptedException ignored){}
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void clickAt(int x, int y){
        clickAt(x, y, 1);
    }

    public void clickAt(int x, int y, int count) {
        while (count > 0) {
            device.click(x, y);
            wait(150);
            count--;
        }

        wait(350);
    }

    public void pressNumericKey(int key){
        device.pressKeyCode(key);
        wait(150);
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void press(int x, int y, int duration) {
        swipe(x, y, 0, 0, duration * 50);
    }

    public void swipe(int x, int y, int xTo, int yTo) {
        swipe(x, y, xTo, yTo, swipeSteps);
    }

    public void swipe(int x, int y, int xTo, int yTo, int duration) {
        device.swipe(x, y, x + xTo, y + yTo, duration);
        wait(swipeSteps*6);
    }
    
    /* public void swipe(Point[] path, int duration) {
        device.swipe(path, duration);
    } */

    //----------------------------------------------------------------------------------------------------
    // Driver start stop
    //----------------------------------------------------------------------------------------------------

    private boolean driverStarted = false;

    public String startDriver() {
        driverStarted = true;

        deviceWakeUp();
        executeShell("svc power stayon true");

        launchAtsWidget();

        if(!usbMode) {
            screenCapture = new CaptureScreenServer(this);
            (new Thread(screenCapture)).start();
        }

        return UUID.randomUUID().toString();
    }

    public void stopDriver() {
        if(driverStarted) {
            forceStop("ATS_DRIVER_STOP");
        }
    }

    public void forceStop(String log) {
        driverStarted = false;

        sendLogs(log + "\n");

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

    private void deviceWakeUp() {
        try {
            device.setOrientationNatural();
            device.freezeRotation();
            device.wakeUp();
        }catch(RemoteException ignored){}
    }

    private void deviceSleep() {
        try {
            device.unfreezeRotation();
            device.sleep();
        } catch(RemoteException ignored){}
    }

    public void terminate() {
        runner.stop();
        executeShell("am force-stop com.ats.atsdroid");
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public ApplicationInfo startChannel(String pkg) {
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

    /* public String getActivityName(String pkg){
        final ApplicationInfo app = getApplicationByPackage(pkg);
        if(app != null) {
            return app.getPackageActivityName();
        }
        return "";
    } */

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
        Bitmap screen = getScreenBitmap();
        screen = Bitmap.createBitmap(screen, 0, 0, deviceInfo.getChannelWidth(), deviceInfo.getChannelHeight(), deviceInfo.getMatrix(), true);
        return getBitmapBytes(screen, Bitmap.CompressFormat.JPEG, 66);
    }

    public byte[] getScreenDataHires() {
        return getBitmapBytes(getScreenBitmap(), Bitmap.CompressFormat.PNG, 100);
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
        }catch (IOException e){
            AtsAutomation.sendLogs("Error on Stream close\n");
        }
        return bytes;
    }

    private Bitmap createEmptyBitmap(int width, int height) {

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setHasAlpha(false);

        final Canvas canvas = new Canvas(bitmap);

        final Paint paint = new Paint();
        paint.setARGB(255, 220, 220, 220);
        canvas.drawRect(0, 0, width, height, paint);

        // canvas.translate(0, -statusBarHeight);

        rootElement.drawElements(canvas, context.getResources());
        return bitmap;
    }

    //--------------------------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------

    private final static String EMPTY_DATA = "&empty;";
    private final static String ENTER_KEY = "$KEY-ENTER";
    private final static String TAB_KEY = "$KEY-TAB";

    public AtsResponse executeRequest(RequestType req) {

        JSONObject jsonObject = new JSONObject();
        
        try {
            jsonObject.put("type", req.type);

            if (RequestType.APP.equals(req.type)) {
                if (req.parameters.length > 1) {
                    if (RequestType.START.equals(req.parameters[0])) {
                        try {
                            final ApplicationInfo app = startChannel(req.parameters[1]);
                            if (app != null) {
                                jsonObject.put("status", "0");
                                jsonObject.put("message", "start app : " + app.getPackageName());
                                jsonObject.put("label", app.getLabel());
                                jsonObject.put("icon", app.getIcon());
                                jsonObject.put("version", app.getVersion());

                                activeChannelsCount++;
                            } else {
                                jsonObject.put("status", "-51");
                                jsonObject.put("message", "app package not found : " + req.parameters[1]);
                            }
                        } catch (Exception e) {
                            System.err.println("Ats error : " + e.getMessage());
                        }
                    } else if (RequestType.STOP.equals(req.parameters[0])) {
                        stopChannel(req.parameters[1]);
                        activeChannelsCount--;
                        if (activeChannelsCount == 0) {
                            AtsClient.current = null;
                            sendLogs("ATS_DRIVER_UNLOCKED\n");
                        }

                        jsonObject.put("status", "0");
                        jsonObject.put("message", "stop app : " + req.parameters[1]);
                    } else if (RequestType.SWITCH.equals(req.parameters[0])) {
                        switchChannel(req.parameters[1]);
                        jsonObject.put("status", "0");
                        jsonObject.put("message", "switch app : " + req.parameters[1]);
                    } else if (RequestType.INFO.equals(req.parameters[0])) {
                        final ApplicationInfo app = getApplicationInfo(req.parameters[1]);
                        if (app != null) {
                            jsonObject.put("status", "0");
                            jsonObject.put("info", app.getJson());
                        } else {
                            jsonObject.put("status", "-81");
                            jsonObject.put("message", "app not found : " + req.parameters[1]);
                        }
                    }
                }
            } else if (RequestType.INFO.equals(req.type)) {

                try {
                    DeviceInfo.getInstance().driverInfoBase(jsonObject, device.getDisplayHeight());

                    jsonObject.put("status", "0");
                    jsonObject.put("message", "device capabilities");
                    jsonObject.put("id", DeviceInfo.getInstance().getDeviceId());

                    String model = DeviceInfo.getInstance().getModel();
                    if (model.startsWith("GM")) {
                        String[] parameters = model.split("_");
                        model = parameters[2];
                    }

                    jsonObject.put("model", model);
                    jsonObject.put("manufacturer", DeviceInfo.getInstance().getManufacturer());
                    jsonObject.put("brand", DeviceInfo.getInstance().getBrand());
                    jsonObject.put("version", DeviceInfo.getInstance().getVersion());
                    jsonObject.put("bluetoothName", DeviceInfo.getInstance().getBtAdapter());

                    List<ApplicationInfo> apps = getApplications();

                    JSONArray applications = new JSONArray();
                    for (ApplicationInfo appInfo : apps) {
                        applications.put(appInfo.getJson());
                    }

                    jsonObject.put("applications", applications);

                } catch (Exception e) {
                    AtsAutomation.sendLogs("Error when getting device info:" + e.getMessage() + "\n");
                    jsonObject.put("status", "-99");
                    jsonObject.put("message", e.getMessage());
                }

            } else if (RequestType.DRIVER.equals(req.type)) {

                if (AtsClient.current != null) {
                    if (req.token == null) {
                        jsonObject.put("message", "Device already in use : " + AtsClient.current.userAgent);
                        jsonObject.put("status", "-20");
                        return new AtsResponseJSON(jsonObject);
                    } else {
                        if (!req.token.equals(AtsClient.current.token)) {
                            jsonObject.put("message", "Device already in use : " + AtsClient.current.userAgent);
                            jsonObject.put("status", "-20");
                            return new AtsResponseJSON(jsonObject);
                        }
                    }
                }

                if (req.parameters.length > 0) {
                    if (RequestType.START.equals(req.parameters[0])) {
    
                        String token = startDriver();
                        jsonObject.put("token", token);
                        jsonObject.put("status", "0");
                        DeviceInfo.getInstance().driverInfoBase(jsonObject, device.getDisplayHeight());
    
                        AtsClient.current = new AtsClient(token, req.userAgent,null);
                        sendLogs("ATS_DRIVER_LOCKED_BY: " + req.userAgent + "\n");
    
                        if (usbMode) {
                            int screenCapturePort = ((AtsRunnerUsb)runner).udpPort;
                            jsonObject.put("screenCapturePort", screenCapturePort);
                        } else {
                            jsonObject.put("screenCapturePort", screenCapture.getPort());
                        }
    
                    } else if (RequestType.STOP.equals(req.parameters[0])) {

                        stopDriver();
                        AtsClient.current = null;
                        activeChannelsCount = 0;
                        sendLogs("ATS_DRIVER_UNLOCKED\n");

                        jsonObject.put("status", "0");
                        jsonObject.put("message", "stop ats driver");

                    } else if (RequestType.QUIT.equals(req.parameters[0])) {

                        stopDriver();
                        jsonObject.put("status", "0");
                        jsonObject.put("message", "close ats driver");

                        terminate();

                        return new AtsResponseJSON(jsonObject);
                    } else {
                        jsonObject.put("status", "-42");
                        jsonObject.put("message", "wrong driver action type : " + req.parameters[0]);
                    }
                } else {
                    jsonObject.put("status", "-41");
                    jsonObject.put("message", "missing driver action");
                }

            } else if (RequestType.SYS_BUTTON.equals(req.type)) {
                if (req.parameters.length == 1) {
                    String button = req.parameters[0];
                    try {
                        boolean pressed = SysButton.pressButtonType(button);
                        if (pressed) {
                            jsonObject.put("status", "0");
                            jsonObject.put("message", "button : " + TextUtils.join(", ", req.parameters));
                        } else {
                            jsonObject.put("status", "-31");
                            jsonObject.put("message", "unknown button type");
                        }
                    } catch (IllegalArgumentException e) {
                        jsonObject.put("status", "-31");
                        jsonObject.put("message", "unknown button type");
                    }
                } else {
                    jsonObject.put("status", "-31");
                    jsonObject.put("message", "missing button type");
                }
            }
            
            else if (RequestType.SYS_PROPERTY_GET.equals(req.type)) {
                if (req.parameters.length == 1) {
                    String propertyName = req.parameters[0];
                    try {
                        String value = Sysprop.getPropertyValue(propertyName);
                        jsonObject.put("message", value);
                        jsonObject.put("status", "0");
                    } catch (IllegalStateException | IllegalArgumentException | JSONException e) {
                        jsonObject.put("message", "unknown property");
                        jsonObject.put("status", "-1");
                    }
                } else {
                    jsonObject.put("status", "-31");
                    jsonObject.put("message", "missing value parameter");
                }
            }
            
            else if (RequestType.SYS_PROPERTY_SET.equals(req.type)) {
                if (req.parameters.length == 2) {
                    String propertyName = req.parameters[0];
                    String propertyValue = req.parameters[1];
                    Sysprop.setProperty(propertyName, propertyValue);
                    jsonObject.put("message", "set " + propertyName + " value");
                    jsonObject.put("status", "0");
                } else {
                    jsonObject.put("status", "-31");
                    jsonObject.put("message", "missing parameters");
                }
            }
            
            else if (RequestType.CAPTURE.equals(req.type)) {
                reloadRoot();
                jsonObject = getRootObject();
            }
            
            else if (RequestType.ELEMENT.equals(req.type)) {
                if (req.parameters.length > 2) {
                    AbstractAtsElement element;
                    String elementId = req.parameters[0];
                    
                    if (elementId.equals("[root]")) {
                        element = rootElement;
                    } else {
                        element = getElement(elementId);
                    }

                    if (element != null) {

                        if (RequestType.INPUT.equals(req.parameters[1])) {

                            jsonObject.put("status", "0");

                            String text = req.parameters[2];
                            if (EMPTY_DATA.equals(text)) {
                                jsonObject.put("message", "element clear text");
                                element.clearText(this);
                            } else if (ENTER_KEY.equals(text)) {
                                jsonObject.put("message", "press enter on keyboard");
                                enterKeyboard();
                            } else if (TAB_KEY.equals(text)) {
                                jsonObject.put("message", "hide keyboard");
                                hideKeyboard();
                            } else {
                                element.inputText(this, text);
                                jsonObject.put("message", "element send keys : " + text);
                            }
                        }

                        else if (RequestType.SCRIPTING.equals(req.parameters[1])) {
                            String script = req.parameters[2];
                            ScriptingExecutor executor = new ScriptingExecutor(this, script);

                            try {
                                String value = executor.execute(element);
                                if (value == null) {
                                    jsonObject.put("message", "scripting on element");
                                } else {
                                    jsonObject.put("message", value);
                                }

                                jsonObject.put("status", "0");
                            } catch (Throwable e) {
                                jsonObject.put("status", "-13");
                                jsonObject.put("message", e.getMessage());
                            }
                        }

                        else if (RequestType.PRESS.equals(req.parameters[1])) {
                            String[] info = req.parameters[2].split(":");
                            List<MotionEvent.PointerCoords[]> pointerCoords = new ArrayList<>();
                            for (String pathInfo : info) {
                                MotionEvent.PointerCoords[] coords = parsePath(pathInfo);
                                pointerCoords.add(coords);
                            }
                            
                            MotionEvent.PointerCoords[][] array = pointerCoords.toArray(new MotionEvent.PointerCoords[][] {});
                            if (array.length > 1) {
                                UiObject object = device.findObject(new UiSelector());
                                if (object.performMultiPointerGesture(array)) {
                                    jsonObject.put("status", "0");
                                    jsonObject.put("message", "press on element");
                                } else {
                                    jsonObject.put("status", "-1");
                                    jsonObject.put("message", "unknown error");
                                }
                            } else {
                                jsonObject.put("status", "-1");
                                jsonObject.put("message", "not enough touches");
                            }
                        }

                        else {
                            int offsetX = 0;
                            int offsetY = 0;

                            if (req.parameters.length > 3) {
                                try {
                                    offsetX = Integer.parseInt(req.parameters[2]);
                                    offsetY = Integer.parseInt(req.parameters[3]);
                                } catch (NumberFormatException e) {
                                    AtsAutomation.sendLogs("Error not enough parameters:" + e.getMessage() + "\n");
                                }
                            }

                            if (RequestType.TAP.equals(req.parameters[1])) {
                                element.click(this, offsetX, offsetY);

                                jsonObject.put("status", "0");
                                jsonObject.put("message", "click on element");

                            } else if (RequestType.SWIPE.equals(req.parameters[1])) {
                                int directionX = 0;
                                int directionY = 0;
                                if (req.parameters.length > 5) {
                                    try {
                                        directionX = Integer.parseInt(req.parameters[4]);
                                        directionY = Integer.parseInt(req.parameters[5]);
                                    } catch (NumberFormatException e) {
                                        AtsAutomation.sendLogs("Error not enough parameters:" + e.getMessage() + "\n");
                                    }
                                }
                                element.swipe(this, offsetX, offsetY, directionX, directionY);
                                jsonObject.put("status", "0");
                                jsonObject.put("message", "swipe element to " + directionX + ":" + directionY);
                            }
                        }
                    } else {
                        jsonObject.put("status", "-22");
                        jsonObject.put("message", "element not found");
                    }
                } else {
                    jsonObject.put("status", "-21");
                    jsonObject.put("message", "missing element id");
                }
            }

            else if (RequestType.SCREENSHOT.equals(req.type)) {
                if (req.parameters.length > 0 && req.parameters[0].indexOf(RequestType.SCREENSHOT_HIRES) == 0) {
                    return new AtsResponseBinary(getScreenDataHires());
                } else {
                    return new AtsResponseBinary(getScreenData());
                }
            }

            else {
                jsonObject.put("status", "-12");
                jsonObject.put("message", "unknown command : " + req.type);
            }

        } catch (JSONException e) {
            sendLogs("Json Error -> " + e.getMessage() + "\n");
        }  catch (Exception e) {
            e.printStackTrace();
        }
    
        return new AtsResponseJSON(jsonObject);
    }
    
    private MotionEvent.PointerCoords[] parsePath(String pathInfo) {
        ArrayList<MotionEvent.PointerCoords> coordsArray = new ArrayList<>();
        String[] coordinatesString = pathInfo.split(";");
        for (String coordinateInfo: coordinatesString) {
            String[] elements = coordinateInfo.split(",");
            int x = Integer.parseInt(elements[0]);
            int y = Integer.parseInt(elements[1]);
            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.x = x;
            coords.y = y;
            coords.pressure = 1;
            coords.size = 1;
            
            coordsArray.add(coords);
        }
    
        return coordsArray.toArray(new MotionEvent.PointerCoords[0]);
    }
    
    public boolean performMultiPointerGesture(int duration, MotionEvent.PointerCoords[]... touches) {
        boolean ret = true;
        
        // Get the pointer with the max steps to inject.
        int maxSteps = 0;
        for (int x = 0; x < touches.length; x++) {
            maxSteps = (maxSteps < touches[x].length) ? touches[x].length : maxSteps;
        }
        
        // specify the properties for each pointer as finger touch
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[touches.length];
        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[touches.length];
        for (int x = 0; x < touches.length; x++) {
            MotionEvent.PointerProperties prop = new MotionEvent.PointerProperties();
            prop.id = x;
            prop.toolType = MotionEvent.TOOL_TYPE_FINGER;
            properties[x] = prop;
            // for each pointer set the first coordinates for touch down
            pointerCoords[x] = touches[x][0];
        }
        
        // Touch down all pointers
        long downTime = SystemClock.uptimeMillis();
        MotionEvent event;
        event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 1,
                properties, pointerCoords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        ret &= injectEventSync(event);
        for (int x = 1; x < touches.length; x++) {
            event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), getPointerAction(MotionEvent.ACTION_POINTER_DOWN, x), x + 1,
                    properties, pointerCoords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            ret &= injectEventSync(event);
        }
        
        // Move all pointers
        for (int i = 1; i < maxSteps - 1; i++) {
            // for each pointer
            for (int x = 0; x < touches.length; x++) {
                // check if it has coordinates to move
                if (touches[x].length > i)
                    pointerCoords[x] = touches[x][i];
                else
                    pointerCoords[x] = touches[x][touches[x].length - 1];
            }
            event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_MOVE, touches.length, properties, pointerCoords, 0, 0, 1, 1,
                    0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            ret &= injectEventSync(event);
            SystemClock.sleep(MOTION_EVENT_INJECTION_DELAY_MILLIS);
        }
        
        // For each pointer get the last coordinates
        for (int x = 0; x < touches.length; x++)
            pointerCoords[x] = touches[x][touches[x].length - 1];
        
        // touch up
        for (int x = 1; x < touches.length; x++) {
            event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                    getPointerAction(MotionEvent.ACTION_POINTER_UP, x), x + 1, properties,
                    pointerCoords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            ret &= injectEventSync(event);
        }
        Log.i(LOG_TAG, "x " + pointerCoords[0].x);
        
        // first to touch down is last up
        event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 1,
                properties, pointerCoords, 0, 0, 1, 1, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        ret &= injectEventSync(event);
        return ret;
    }
    
    private static final String LOG_TAG = AtsAutomation.class.getSimpleName();
    private static final int MOTION_EVENT_INJECTION_DELAY_MILLIS = 500;
    
    private int getPointerAction(int motionEvent, int index) {
        return motionEvent + (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
    }
    
    private boolean injectEventSync(InputEvent event) {
        return automation.injectInputEvent(event, true);
    }
}