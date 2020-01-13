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
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.Configurator;
import android.support.test.uiautomator.UiDevice;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ats.atsdroid.AtsRunner;
import com.ats.atsdroid.element.AbstractAtsElement;
import com.ats.atsdroid.element.AtsRootElement;
import com.ats.atsdroid.ui.AtsActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
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
    private final UiAutomation automation = instrument.getUiAutomation();
    private final UiDevice device = UiDevice.getInstance(instrument);
    //private final Context context = InstrumentationRegistry.getContext();
    private final Context context = InstrumentationRegistry.getTargetContext();

    private Matrix matrix;

    private List<ApplicationInfo> applications;
    private AtsRootElement rootElement;
    private DriverThread driverThread;

    //-------------------------------------------------------
    //assume here orientation is portrait
    //-------------------------------------------------------

    private int channelWidth;
    private int channelHeight;

    //-------------------------------------------------------
    private AbstractAtsElement found = null;
    private Bitmap compressedScreen;

    public AtsRunner runner;
    public int port;

    public AtsAutomation(int port, AtsRunner runner){
        this.runner = runner;
        this.port = port;
        AtsActivity.setAutomation(this);

        try {
            device.setOrientationNatural();
            device.freezeRotation();
        }catch (RemoteException e){}

        Configurator.getInstance().setWaitForIdleTimeout(0);

        /*int barHeight = 0;
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            barHeight = context.getResources().getDimensionPixelSize(resourceId);
        }*/

        DeviceInfo.getInstance().initData(port, device.getDisplayWidth(), device.getDisplayHeight());

        channelWidth = DeviceInfo.getInstance().getResolutionWidth();
        channelHeight = DeviceInfo.getInstance().getResolutionHeight();
        matrix = DeviceInfo.getInstance().getMatrixScale();

        //-------------------------------------------------------------
        // Bitmap factory default
        // -------------------------------------------------------------
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        //-------------------------------------------------------------

        device.pressHome();
        launchAtsWidget();
        reloadRoot();
        loadApplications();

        sleep();
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
            wait(200);
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

   /* public int getChannelX(){
        return channelX;
    }

    public int getChannelY(){
        return channelY;
    }*/

    public List<ApplicationInfo> getApplications(){
        return applications;
    }

    public ApplicationInfo getApplicationInfo(String pkg){
        return getApplicationByPackage(pkg);
    }

    private void loadApplications(){

        applications = new ArrayList<ApplicationInfo>();

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
                    } catch (PackageManager.NameNotFoundException e) {}

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
            if (app.samePackage(pkg)) {
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

    public void clickAt(int x, int y){
        device.click(x, y);
        wait(500);
    }

    public void pressNumericKey(int key){
        device.pressKeyCode(key);
        wait(150);
    }

    public void highlightElement(Rect bounds){

        //context.startService( new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:com.ats.atsdroid.ui"), context, HighlightService.class));

        //context.startService( new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:com.ats.atsdroid"), context, HighlightService.class));
        //context.st

        /*final Intent highlightIntent = new Intent();
        highlightIntent.setClassName(context, "com.ats.atsdroid.ui.HighlightActivityx");
        highlightIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        highlightIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        highlightIntent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        highlightIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        highlightIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        highlightIntent.putExtra(HighlightActivityx.ELEMENT_BOUNDS, new int[]{bounds.left, bounds.top, bounds.width(), bounds.height()});
        context.startActivity(highlightIntent);*/
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void swipe(int x, int y, int xTo, int yTo){
        device.swipe(x, y, x + xTo, y + yTo, swipeSteps);
        wait(swipeSteps*6);
    }

    public int getScreenCapturePort(){
        if(driverThread != null){
            return driverThread.getScreenCapturePort();
        }
        return -1;
    }

    public void startDriverThread(){
        stopDriverThread();

        wakeUp();
        launchAtsWidget();

        driverThread = new DriverThread(this);
        (new Thread(driverThread)).start();
    }

    public void stopDriverThread(){

        launchAtsWidget();

        if(driverThread != null){
            driverThread.stop();
        }
    }

    public void wakeUp(){
        try {
            device.wakeUp();
        }catch(RemoteException e){}
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void sleep(){
        try {
            device.sleep();
        }catch(RemoteException e){}
    }

    public void terminate(){
        executeShell("am force-stop com.ats.atsdroid");
    }

    public ApplicationInfo startChannel(String pkg){
        final ApplicationInfo app = getApplicationByPackage(pkg);
        if(app != null) {
            executeShell("am start -W -S --activity-brought-to-front --activity-multiple-task --activity-no-animation --activity-no-history -n " + app.getPackageActivityName());
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

        final ApplicationInfo app = getApplicationByPackage(pkg);
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
        launchAtsWidget();
    }

    //----------------------------------------------------------------------------------------------------
    // Screen capture
    //----------------------------------------------------------------------------------------------------

    private void stopActivity(String pkg){
        if(pkg != null){
            executeShell("am force-stop " + pkg);
        }
    }

    public byte[] getScreenData() {
        final Bitmap screen = automation.takeScreenshot();
        if (screen == null) {
            compressedScreen = createEmptyBitmap(channelWidth, channelHeight, Color.LTGRAY);
        } else {
            compressedScreen = Bitmap.createBitmap(screen, 0, 0, channelWidth, channelHeight, matrix, true);
            screen.recycle();
        }

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        compressedScreen.compress(Bitmap.CompressFormat.JPEG, 55, outputStream);
        compressedScreen.recycle();
        byte[] bytes = outputStream.toByteArray();
        return bytes;
    }

    public byte[] getScreenDataHires(boolean lostLess) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final Bitmap screen = automation.takeScreenshot();
        byte[] bytes;

        if(lostLess) {
            screen.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            screen.recycle();
        } else {
            if (screen == null) {
                compressedScreen = createEmptyBitmap(channelWidth, channelHeight, Color.LTGRAY);
            } else {
                compressedScreen = Bitmap.createBitmap(screen, 0, 0, channelWidth, channelHeight, null, true);
                screen.recycle();
            }
            compressedScreen.compress(Bitmap.CompressFormat.PNG, 55, outputStream);
            compressedScreen.recycle();
        }
        bytes = outputStream.toByteArray();
        return bytes;
    }

    private Bitmap createEmptyBitmap(int width, int height, int color) {

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
}