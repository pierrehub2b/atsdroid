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
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
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
import java.util.Timer;
import java.util.TimerTask;

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
    private int bmpQuality = 100;
    private Bitmap.CompressFormat bmpCompress = Bitmap.CompressFormat.PNG;

    private List<ApplicationInfo> applications;

    private Timer awakeTimer;

    private AtsRootElement rootElement;

    private CaptureScreenServer screenCapture;

    //-------------------------------------------------------
    //assume here orientation is portrait
    //-------------------------------------------------------
    private int channelX = 0;
    private int channelWidth = device.getDisplayWidth();

    private int channelY;
    private int channelHeight;
    //-------------------------------------------------------

    public AtsAutomation(){

        Configurator.getInstance().setWaitForIdleTimeout(0);
        deviceInfo.initSize(device.getDisplayWidth(), device.getDisplayHeight());

        try {
            device.setOrientationNatural();
            device.freezeRotation();
        }catch (RemoteException e){}

        device.pressHome();
        executeShell("am start -W com.ats.atsdroid/.ui.AtsActivity");

        reloadRoot();

        channelY = rootElement.getChannelY();
        channelHeight = rootElement.getChannelHeight();

        matrix.postScale((float)DeviceInfo.getInstance().getDeviceWidth() / (float)channelWidth, (float)DeviceInfo.getInstance().getDeviceHeight() / (float)channelHeight);

        this.screenCapture = new CaptureScreenServer(this);
        (new Thread(this.screenCapture)).start();

        deviceSleep();
    }

    public void reloadRoot(){

        AccessibilityNodeInfo rootNode = automation.getRootInActiveWindow();
        while (rootNode == null){
            sleep(100);
            rootNode = automation.getRootInActiveWindow();
        }
        rootNode.refresh();

        try {
            rootElement = new AtsRootElement(device, rootNode);
        }catch (Exception e){
            sleep(100);
            reloadRoot();
        }
    }

    private AbstractAtsElement found = null;
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
        loadApplications();
        return applications;
    }

    private void loadApplications(){

        applications = new ArrayList<ApplicationInfo>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> pkgAppsList = context.getPackageManager().queryIntentActivities( mainIntent, 0);

        for(ResolveInfo info : pkgAppsList){

            String pkg = info.activityInfo.packageName;
            String act = info.activityInfo.name;

            ApplicationInfo app = getApplicationByPackage(pkg);
            if(app != null){
                app.addActivity(act);
            }else{

                CharSequence label = info.activityInfo.nonLocalizedLabel;
                Drawable icon = null;

                try{
                    icon = context.getPackageManager().getApplicationIcon(pkg);
                }catch (PackageManager.NameNotFoundException e){}

                applications.add(new ApplicationInfo(pkg, act, label, icon));
            }
        }
    }

    private ApplicationInfo getApplicationByPackage(String pkg){
        for (ApplicationInfo app : applications) {
            if (app.getPackageName().equals(pkg)) {
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

    public void deviceWakeUp(){
        awakeTimer = new Timer(true);
        awakeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    device.wakeUp();
                }catch(RemoteException e){}
            }
        }, 0, 2000);
        executeShell("am start -W com.ats.atsdroid/.ui.AtsActivity");
    }

    public void deviceSleep(){
        if(awakeTimer != null) {
            awakeTimer.cancel();
            awakeTimer.purge();
            awakeTimer = null;
        }

        try {
            device.sleep();
        }catch(RemoteException e){}
    }

    private void executeShell(String value){
        try {
            device.executeShellCommand(value);
        }catch(Exception e){}
    }

    public void sleep(int ms){
        try {
            Thread.sleep(ms);
        }catch(InterruptedException e){}
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void clickAt(int x, int y){
        device.click(x, y);
        sleep(500);
    }

    public void sendNumericKeys(String value){
        device.pressKeyCode(KeyEvent.KEYCODE_NUMPAD_2);
        device.pressKeyCode(KeyEvent.KEYCODE_NUMPAD_1);
        device.pressKeyCode(KeyEvent.KEYCODE_NUMPAD_3);
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void terminate(){
        executeShell("am force-stop com.ats.atsdroid");
    }

    public ApplicationInfo startChannel(String pkg){

        loadApplications();
        ApplicationInfo app = getApplicationByPackage(pkg);

        if(app != null) {
            stopActivity(pkg);
            context.startActivity(app.getIntent(Intent.FLAG_ACTIVITY_NEW_TASK));

            sleep(2000);
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

        loadApplications();
        ApplicationInfo app = getApplicationByPackage(pkg);

        if(app != null) {

            context.startActivity(app.getIntent(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT));

            sleep(1000);
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

    public int getScreenCapturePort(){
        return screenCapture.getPort();
    }

    public byte[] getScreenData()
    {
        Bitmap screen = automation.takeScreenshot();
        if(screen == null) {
            screen = createEmptyBitmap(channelWidth + channelX, channelHeight + channelY, Color.LTGRAY);
        }
        screen = Bitmap.createBitmap(screen, channelX, channelY, channelWidth, channelHeight, matrix, true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        screen.compress(bmpCompress, bmpQuality, outputStream);

        byte[] result = outputStream.toByteArray();
        try{
            outputStream.close();
        }catch(Exception e){}

        screen.recycle();

        return result;
    }

    public void setQuality(int level){
        if(level == 3){
            bmpQuality = 100;
            bmpCompress = Bitmap.CompressFormat.PNG;
        }else{
            bmpCompress = Bitmap.CompressFormat.JPEG;
            if(level == 2){
                bmpQuality = 80;
            }else if(level == 1){
                bmpQuality = 60;
            }else{
                bmpQuality = 40;
            }
        }
    }

    public static Bitmap createEmptyBitmap(int width, int height, int color) {

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(0F, 0F, (float) width, (float) height, paint);

        bitmap.setHasAlpha(false);

        return bitmap;
    }
}