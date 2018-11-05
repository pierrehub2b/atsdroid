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
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.Configurator;
import android.support.test.uiautomator.UiDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ats.atsdroid.element.AbstractAtsElement;
import com.ats.atsdroid.element.AtsRootElement;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class AtsAutomation {

    public static final String HOME = "home";
    public static final String BACK = "back";
    public static final String ENTER = "enter";
    public static final String MENU = "menu";
    public static final String SEARCH = "search";

    private final Instrumentation instrument = InstrumentationRegistry.getInstrumentation();
    private final UiAutomation automation = instrument.getUiAutomation();
    private final UiDevice device = UiDevice.getInstance(instrument);
    private final Context context = InstrumentationRegistry.getContext();

    private final DeviceInfo deviceInfo = DeviceInfo.getInstance();

    private final Matrix matrix = new Matrix();
    private float scaleX = 0;
    private float scaleY = 0;

    private List<ApplicationInfo> applications;

    private AtsRootElement rootElement;

    private DriverThread driverThread;

    //-------------------------------------------------------
    //assume here orientation is portrait
    //-------------------------------------------------------
    private int channelX = 0;
    private int channelWidth = device.getDisplayWidth();

    private int channelY;
    private int channelHeight;
    //-------------------------------------------------------
    private AbstractAtsElement found = null;

    public AtsAutomation(int port){

        Configurator.getInstance().setWaitForIdleTimeout(0);
        deviceInfo.initData(port, device.getDisplayWidth(), device.getDisplayHeight());

        try {
            device.setOrientationNatural();
            device.freezeRotation();
        }catch (RemoteException e){}

        device.pressHome();
        executeShell("am start -W com.ats.atsdroid/.ui.AtsActivity");

        loadApplications();
        reloadRoot();

        channelY = rootElement.getChannelY();
        channelHeight = rootElement.getChannelHeight();

        scaleX = (float)DeviceInfo.getInstance().getDeviceWidth() / (float)channelWidth;
        scaleY = (float)DeviceInfo.getInstance().getDeviceHeight() / (float)channelHeight;
        matrix.preScale(scaleX, scaleY);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        sleep();
    }

    public void reloadRoot(){

        AccessibilityNodeInfo rootNode = automation.getRootInActiveWindow();
        while (rootNode == null){
            wait(100);
            rootNode = automation.getRootInActiveWindow();
        }
        rootNode.refresh();

        try {
            rootElement = new AtsRootElement(device, rootNode);
        }catch (Exception e){
            wait(100);
            reloadRoot();
        }
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

    public int getChannelWidth(){
        return channelWidth;
    }

    public int getChannelHeight(){
        return channelHeight;
    }

    public int getChannelX(){
        return channelX;
    }

    public int getChannelY(){
        return channelY;
    }

    public List<ApplicationInfo> getApplications(){
        return applications;
    }

    public ApplicationInfo getApplicationInfo(String pkg){
        return getApplicationByPackage(pkg);
    }

    private void loadApplications(){

        applications = new ArrayList<ApplicationInfo>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> pkgAppsList = context.getPackageManager().queryIntentActivities( mainIntent, 0);

        for(ResolveInfo info : pkgAppsList){

            ActivityInfo activity = info.activityInfo;
            String pkg = activity.packageName;
            String act = activity.name;

            if(pkg != null && act != null){

                ApplicationInfo app = getApplicationByPackage(pkg);
                if(app != null){
                    app.addActivity(act);
                }else {

                    applications.add(new ApplicationInfo(
                            pkg,
                            act,
                            (activity.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                            activity.loadLabel(context.getPackageManager()),
                            activity.loadIcon(context.getPackageManager())));
                }
            }
        }
    }

    private ApplicationInfo getApplicationByPackage(String pkg){
        for (ApplicationInfo app : applications) {
            if (app.samePackage(pkg)) {
                return app;
            }
        }
        return null;
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

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
        }
    }

    private void executeShell(String value){
        try {
            device.executeShellCommand(value);
        }catch(Exception e){}
    }

    public void wait(int ms){
        try {
            Thread.sleep(ms);
        }catch(InterruptedException e){}
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void clickAt(int x, int y, boolean hideKeyboard){
        device.click(x, y);
        if(hideKeyboard){
            device.pressKeyCode(KeyEvent.KEYCODE_ESCAPE);
            device.pressKeyCode(KeyEvent.KEYCODE_CLEAR);
        }
        wait(500);
    }

    public void pressKey(int code){
        device.pressKeyCode(code);
    }

    public void sendNumericKeys(String value){

        device.pressKeyCode(KeyEvent.KEYCODE_P);
        device.pressKeyCode(KeyEvent.KEYCODE_I);
        device.pressKeyCode(KeyEvent.KEYCODE_E);
        device.pressKeyCode(KeyEvent.KEYCODE_R);
        device.pressKeyCode(KeyEvent.KEYCODE_R);
        device.pressKeyCode(KeyEvent.KEYCODE_E);



        /*device.pressKeyCode(KeyEvent.KEYCODE_NUMPAD_2);
        device.pressKeyCode(KeyEvent.KEYCODE_NUMPAD_1);
        device.pressKeyCode(KeyEvent.KEYCODE_NUMPAD_0);
        device.pressKeyCode(KeyEvent.KEYCODE_NUMPAD_6);
        device.pressKeyCode(KeyEvent.KEYCODE_NUMPAD_1);
        device.pressKeyCode(KeyEvent.KEYCODE_NUMPAD_9);
        device.pressKeyCode(KeyEvent.KEYCODE_NUMPAD_6);
        device.pressKeyCode(KeyEvent.KEYCODE_NUMPAD_7);*/

    }

    public void swipe(int x, int y, int xTo, int yTo){
        device.swipe(x, y, x + xTo, y + yTo, 10);
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public int getScreenCapturePort(){
        if(driverThread != null){
            return driverThread.getScreenCapturePort();
        }
        return -1;
    }

    public void startDriverThread(){
        stopDriverThread();

        wakeUp();
        executeShell("am start -W com.ats.atsdroid/.ui.AtsActivity");

        driverThread = new DriverThread(this);
        (new Thread(driverThread)).start();
    }

    public void stopDriverThread(){
        if(driverThread != null){
            driverThread.stop();
        }
    }

    public void wakeUp(){
        try {
            device.wakeUp();
        }catch(RemoteException e){}
    }

    public void sleep(){
        try {
            device.sleep();
        }catch(RemoteException e){}
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void terminate(){
        executeShell("am force-stop com.ats.atsdroid");
    }

    public ApplicationInfo startChannel(String pkg){

        final ApplicationInfo app = getApplicationByPackage(pkg);

        if(app != null) {
            stopActivity(pkg);

            app.start(context);

            wait(2000);
            device.waitForIdle();
            device.waitForWindowUpdate(null, 2000);

            reloadRoot();
        }
        return app;
    }

    public void switchChannel(String pkg){

        AccessibilityNodeInfo rootNode = null;
        while (rootNode == null){
            rootNode = automation.getRootInActiveWindow();
        }
        if(pkg.contentEquals(rootNode.getPackageName())){
            return;
        }

        ApplicationInfo app = getApplicationByPackage(pkg);

        if(app != null) {

            app.toFront(context);

            wait(1000);
            device.waitForIdle();
            device.waitForWindowUpdate(null, 2000);

            reloadRoot();
        }
    }

    public void stopChannel(String pkg){
        stopActivity(pkg);
    }

    private void stopActivity(String pkg){
        if(pkg != null){
            executeShell("am force-stop " + pkg);
        }
    }

    //----------------------------------------------------------------------------------------------------
    // Screen capture
    //----------------------------------------------------------------------------------------------------

    public byte[] getScreenData()
    {
        Bitmap screen = automation.takeScreenshot();
        Bitmap resizedScreen = null;
        if(screen == null) {
            resizedScreen = createEmptyBitmap(channelWidth, channelHeight, Color.LTGRAY);
        }else{
            resizedScreen = Bitmap.createBitmap(screen, channelX, channelY, channelWidth, channelHeight, matrix, true);
            screen.recycle();
        }

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        resizedScreen.compress(Bitmap.CompressFormat.JPEG, 32, outputStream);
        resizedScreen.recycle();

        return outputStream.toByteArray();
    }

    private Bitmap createEmptyBitmap(int width, int height, int color) {

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setHasAlpha(false);

        final Canvas canvas = new Canvas(bitmap);

        final Paint paint = new Paint();
        paint.setARGB(255, 220, 220, 220);
        canvas.drawRect(0, 0, width, height, paint);

        canvas.translate(-channelX, -channelY);

        rootElement.drawElements(canvas, context.getResources());
        return bitmap;
    }
}