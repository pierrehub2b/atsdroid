package com.ats.atsdroid.utils;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ApplicationInfo {

    private String packageName;
    private List<String> activities = new ArrayList<String>();
    private String label;
    private Bitmap icon;

    public ApplicationInfo(String pkg, String act, CharSequence label, Drawable icon){
        this.packageName = pkg;
        this.activities.add(act);

        if(label != null){
            this.label = label.toString();
        }

        if(icon != null){
            try {
                this.icon = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }catch(Exception e){}
        }
    }

    public String getIcon(){
        if(icon != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        }
        return "";
    }

    public String getPackageName() {
        return packageName;
    }

    public String getLabel(){
        if(label == null){
            return packageName;
        }
        return label;
    }

    public String getActivity(int index) {
        return activities.get(index);
    }

    public void addActivity(String act){
        activities.add(act);
    }

    public Intent getIntent(int flag){
        Intent intent = new Intent();
        intent.setClassName(packageName, getActivity(0));
        intent.addFlags(flag);
        return intent;
    }
}