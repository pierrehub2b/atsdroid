package com.ats.atsdroid.element;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.view.accessibility.AccessibilityNodeInfo;

import com.ats.atsdroid.utils.AtsAutomation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public abstract class AbstractAtsElement {

    protected String id = UUID.randomUUID().toString();
    protected String tag;
    protected int index;
    protected Rect bounds = new Rect(0,0,0,0);

    protected boolean numeric;

    protected AccessibilityNodeInfo node;
    protected AbstractAtsElement[] children;

    public void loadBounds(AccessibilityNodeInfo node){
        node.getBoundsInScreen(bounds);
    }

    public synchronized void loadChildren(AccessibilityNodeInfo node){

        this.node = node;

        int count = node.getChildCount();
        children = new AbstractAtsElement[count];
        for(int i=0; i<count; i++){
            children[i] = new AtsElement(node.getChild(i), i);
        }
    }

    public AbstractAtsElement[] getChildren(){
        return children;
    }

    public String getId(){
        return id;
    }

    public int getHeight(){
        return bounds.height();
    }

    //--------------------------------------------------------------------------------------------
    //
    //--------------------------------------------------------------------------------------------

    public String getViewId(){return null;}

    public void click(AtsAutomation automation, int offsetX, int offsetY){

        node.refresh();
        node.getBoundsInScreen(bounds);

        automation.clickAt(bounds.left + offsetX, bounds.top + offsetY);
    }

    public void inputText(AtsAutomation automation, String value){
        node.refresh();
        if(numeric){
            automation.sendNumericKeys(value);
        }else {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        }
    }

    //--------------------------------------------------------------------------------------------
    //
    //--------------------------------------------------------------------------------------------

    public JSONObject getJsonObject(){
        JSONObject props = new JSONObject();
        try{
            props.put("id", id);
            props.put("tag", tag);
            props.put("index", index);
            props.put("x", bounds.left);
            props.put("y", bounds.top);
            props.put("width", bounds.width());
            props.put("height", bounds.height());

            JSONArray childrenArray = new JSONArray();
            for (AbstractAtsElement child : children){
                childrenArray.put(child.getJsonObject());
            }
            props.put("children", childrenArray);

        } catch (JSONException e) {}
        return props;
    }
}