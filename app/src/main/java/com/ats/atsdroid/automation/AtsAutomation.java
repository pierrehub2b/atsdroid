package com.ats.atsdroid.automation;

import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.util.Base64;

import com.ats.atsdroid.DeviceInfo;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.regex.Pattern;

public class AtsAutomation {

    private UiAutomation automation;
    private UiDevice device;
    private Matrix matrix = new Matrix();
    private int width;
    private int height;
    private int quality;

    private String startAppPackageName;

    private String activePackageName;
    private Pattern activePackagePattern;

    public AtsAutomation(float scale, int quality){

        this.automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        this.device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        this.matrix.postScale(scale, scale);
        this.quality = quality;
        this.width = device.getDisplayWidth();
        this.height = device.getDisplayHeight();

        DeviceInfo.getInstance().initData(width, height);

        try {
            device.wakeUp();
        }catch(RemoteException e){}

        executeShell("am start -W com.ats.atsdroid/.AtsActivity");
        reloadActivePackage(null);
    }

    private void reloadActivePackage(String name){
        if(name == null){
            activePackageName = automation.getRootInActiveWindow().getPackageName().toString() + ":id/";
        }else{
            activePackageName = name + ":id/";
        }
        activePackagePattern = Pattern.compile("^" + activePackageName + ".*");
    }

    private BySelector getByRes(){
        return By.res(activePackagePattern);
    }

    private void executeShell(String value){
        try {
            device.executeShellCommand(value);
        }catch(Exception e){}
    }

    //----------------------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------------------

    public int getPackageLength(){
        return activePackageName.length();
    }

    public void terminate(){
        executeShell("am force-stop com.ats.atsdroid");
    }

    public String startActivity(String packageName, String activity){
        String completeName = packageName + "/" + activity;

        startAppPackageName = packageName;
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
                Bitmap resizedBitmap = Bitmap.createBitmap(screen, 0, 0, width, height, matrix, true);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

                byte[] data = outputStream.toByteArray();
                outputStream.close();
                resizedBitmap.recycle();

                return Base64.encodeToString(data, Base64.DEFAULT);
            }
        }catch(Exception e){}
        return null;
    }

    public List<UiObject2> getAllElements(){
        return device.findObjects(getByRes());
    }

    public UiObject2 getElementByRes(String resId){
        return device.findObject(By.res(resId));
    }
}