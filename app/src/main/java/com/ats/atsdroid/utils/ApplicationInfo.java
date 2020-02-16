package com.ats.atsdroid.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.test.uiautomator.UiDevice;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class ApplicationInfo {

    private String packageName;
    private String activity;
    private String label;
    private String icon;
    private boolean system;
    private String version;

    public ApplicationInfo(String pkg, String act, String ver, Boolean sys, CharSequence lbl, Drawable icon){

        this.packageName = pkg;
        this.version = ver;
        this.activity = act;
        this.system = sys;

        if(lbl != null){
            this.label = lbl.toString();
        }else{
            this.label = pkg;
        }

        if(icon != null){
            this.icon = drawableToBase64(icon);
        }
    }

    public void start(Context context, UiDevice device){
        final Intent startChannel = getIntent(
                Intent.FLAG_ACTIVITY_NEW_TASK,
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT,
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
                Intent.FLAG_ACTIVITY_NO_ANIMATION,
                Intent.FLAG_ACTIVITY_NO_HISTORY,
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
                Intent.FLAG_INCLUDE_STOPPED_PACKAGES
        );
        context.startActivity(startChannel);
        device.waitForWindowUpdate(packageName, 3000);
    }

    public boolean packageEquals(String value){
        return packageName.equals(value);
    }

    public String getIcon() {
        return icon;
    }

    public String getLabel() {
        return label;
    }
    public String getVersion() {
        return version;
    }

    public String getPackageName(){
        return packageName;
    }
    public String getPackageActivityName(){
        return packageName + "/" + activity;
    }

    public Intent getIntent(int... flag){
        final Intent intent = new Intent();
        intent.setClassName(packageName, activity);
        for (int i=0; i< flag.length; i++){
            intent.addFlags(flag[i]);
        }
        return intent;
    }

    public JSONObject getJson() throws JSONException{
        JSONObject result = new JSONObject();
        result.put("packageName", packageName);
        result.put("activity", activity);
        result.put("system", system);
        result.put("label", label);
        result.put("icon", icon);
        result.put("version", version);
        result.put("os", "android");
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Bitmap getBitmapFromDrawable (Drawable drawable) {

        if (drawable instanceof AdaptiveIconDrawable) {

            Drawable backgroundDr = ((AdaptiveIconDrawable) drawable).getBackground();
            Drawable foregroundDr = ((AdaptiveIconDrawable) drawable).getForeground();

            Drawable[] drr = new Drawable[2];
            drr[0] = backgroundDr;
            drr[1] = foregroundDr;

            LayerDrawable layerDrawable = new LayerDrawable(drr);

            int width = layerDrawable.getIntrinsicWidth();
            int height = layerDrawable.getIntrinsicHeight();

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);

            layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            layerDrawable.draw(canvas);

            return bitmap;
        }else if(drawable instanceof BitmapDrawable){
            return ((BitmapDrawable)drawable).getBitmap();
        }
        return null;
    }

    public static String drawableToBase64 (Drawable drawable) {

        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT >= 26) {
            bitmap = getBitmapFromDrawable(drawable);
        }else{
            bitmap = ((BitmapDrawable)drawable).getBitmap();
        }

        if(bitmap != null) {

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 32, 32, true);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

            bitmap.recycle();
            scaledBitmap.recycle();

            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
        }else{
            return "";
        }
    }
}