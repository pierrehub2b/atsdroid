package com.ats.atsdroid.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ApplicationInfo {

    private String packageName;
    private List<String> activities = new ArrayList<String>();
    private String label;
    private String icon;
    private boolean system;

    public ApplicationInfo(String pkg, String act, Boolean sys, CharSequence label, Drawable icon){

        this.packageName = pkg;
        this.activities.add(act);
        this.system = sys;

        if(label != null){
            this.label = label.toString();
        }else{
            this.label = pkg;
        }

        if(icon != null){
            this.icon = drawableToBase64(icon);
        }
    }

    public void start(Context context){
        Intent intent = getIntent(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void toFront(Context context){
        context.startActivity(getIntent(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT));
    }

    public boolean samePackage(String value){
        return packageName != null && packageName.equals(value);
    }

    public String getIcon() {
        return icon;
    }

    public String getLabel() {
        return label;
    }

    public String getPackageName(){
        return packageName;
    }

    public String getActivity(int index) {
        return activities.get(index);
    }

    public void addActivity(String act){
        activities.add(act);
    }

    private Intent getIntent(int flag){
        Intent intent = new Intent();
        intent.setClassName(packageName, getActivity(0));
        intent.addFlags(flag);
        return intent;
    }

    public JSONObject getJson() throws JSONException{
        JSONObject result = new JSONObject();
        result.put("packageName", packageName);
        result.put("activity", getActivity(0));
        result.put("system", system);
        result.put("label", label);
        result.put("icon", icon);
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

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 24, 24, true);

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