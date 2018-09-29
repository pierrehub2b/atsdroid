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

package com.ats.atsdroid.automation;

import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.util.Base64;

import com.ats.atsdroid.DeviceInfo;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

public class AtsAutomation {

    private final UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
    private final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

    private final Matrix matrix = new Matrix();
    private final DeviceInfo deviceInfo = DeviceInfo.getInstance();

    private int bmpQuality = 40;
    private Bitmap.CompressFormat bmpCompress = Bitmap.CompressFormat.JPEG;

    private String startAppPackageName;
    private String activePackageName;
    private Pattern activePackagePattern;

    private List<AtsElement> cachedElements = new ArrayList<AtsElement>();
    private Timer awakeTimer;

    public AtsAutomation(){

        deviceInfo.initData(device.getDisplayWidth(), device.getDisplayHeight());
        matrix.postScale(deviceInfo.getWidthScale(), deviceInfo.getHeightScale());

        executeShell("am start -W com.ats.atsdroid/.AtsActivity");
        reloadActivePackage(null);

        sleep();
    }

    public UiDevice getDevice(){
        return device;
    }

    public String getActivePackageName(){
        return activePackageName;
    }

    public void setQuality(int level){
        if(level == 3){
            this.bmpQuality = 100;
            this.bmpCompress = Bitmap.CompressFormat.PNG;
        }else{
            this.bmpCompress = Bitmap.CompressFormat.JPEG;
            if(level == 2){
                this.bmpQuality = 80;
            }else if(level == 1){
                this.bmpQuality = 60;
            }else{
                this.bmpQuality = 40;
            }
        }
    }

    public void wakeUp(){
        awakeTimer = new Timer(true);
        awakeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    device.wakeUp();
                }catch(RemoteException e){}
            }
        }, 0, 2000);
    }

    public void sleep(){
        if(awakeTimer != null) {
            awakeTimer.cancel();
            awakeTimer.purge();
            awakeTimer = null;
        }

        try {
            device.sleep();
        }catch(RemoteException e){}
    }

    private void reloadActivePackage(String name){
        if(name == null){
            activePackageName = automation.getRootInActiveWindow().getPackageName().toString();
        }else{
            activePackageName = name;
        }
        activePackagePattern = Pattern.compile("^" + activePackageName + ".*");
    }

    private void executeShell(String value){
        try {
            device.executeShellCommand(value);
        }catch(Exception e){}
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void clearCachedElements(){
        cachedElements.clear();
    }

    public IAtsElement getCachedElement(String id){
        for(AtsElement elem : cachedElements){
            if(elem.getId().equals(id)){
                return elem;
            }
        }
        return new AtsElementNotFound();
    }

    public JSONObject addCachedElement(UiObject2 element){
        AtsElement atsElem = new AtsElement(this, element);
        cachedElements.add(atsElem);

        return atsElem.toJson();
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void clickAtRect(Rect bounds){
        device.click(bounds.centerX(), bounds.centerY());
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public void terminate(){
        executeShell("am force-stop com.ats.atsdroid");
    }

    public String startActivity(int quality, String packageName, String activity){

        setQuality(quality);
        startAppPackageName = packageName;

        String completeName = packageName + "/" + activity;
        executeShell("am start -S -W " + completeName);

        reloadActivePackage(packageName);
        return completeName;

    }

    public String stopActivity(){
        if(startAppPackageName != null){
            executeShell("am force-stop " + startAppPackageName);
        }
        return startAppPackageName;
    }

    public String getScreenPic()
    {
        try {
            Bitmap screen = automation.takeScreenshot();
            if(screen != null) {
                Bitmap resizedBitmap = Bitmap.createBitmap(screen, 0, 0, deviceInfo.getDisplayWidth(), deviceInfo.getDisplayHeight(), matrix, true);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                resizedBitmap.compress(bmpCompress, bmpQuality, outputStream);

                byte[] data = outputStream.toByteArray();
                outputStream.close();
                resizedBitmap.recycle();

                return Base64.encodeToString(data, Base64.DEFAULT);
            }
        }catch(Exception e){}
        return null;
    }

    public BySelector getSelector(String tag){
        BySelector selector = By.res(activePackagePattern);
        if(!"*".equals(tag)) {
            selector = selector.clazz(Pattern.compile("(?i).*\\." + tag + "$"));
        }
        return selector;
    }
}