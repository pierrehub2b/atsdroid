package com.ats.atsdroid.element;

import android.support.test.uiautomator.UiDevice;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;
import org.json.JSONObject;

public class AtsRootElement extends AbstractAtsElement {

    private int channelY = 0;
    private int channelHeight;

    public AtsRootElement(UiDevice device, AccessibilityNodeInfo node){

        this.tag = "root";
        this.channelHeight = device.getDisplayHeight();

        loadBounds(node);
        loadChildren(node);

        for (AbstractAtsElement child : getChildren()){
            if("statusBarBackground".equals(child.getViewId())){
                this.channelHeight -= child.getHeight();
                this.channelY = child.getHeight();
            //}else if("navigationBarBackground".equals(child.getId())) {
            //    channelHeight += child.getHeight();
            //}   channelHeight += child.getHeight();
            }
        }
    }

    public int getChannelY(){
        return channelY;
    }

    public int getChannelHeight(){
        return channelHeight;
    }

    @Override
    public JSONObject getJsonObject() {
        JSONObject obj = super.getJsonObject();
        try {
            obj.put("channelY", channelY);
            obj.put("channelHeight", channelHeight);
        }catch (JSONException e){}
        return obj;
    }
}