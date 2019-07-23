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

package com.ats.atsdroid.element;

import android.os.Build;
import android.text.InputType;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AtsElement extends AbstractAtsElement {

    private Map<String, String> attributes = new HashMap<String, String>();

    public AtsElement(final AccessibilityNodeInfo node){

        String data = node.getClassName().toString();
        this.tag = data.substring(data.lastIndexOf(".") + 1);
        this.clickable = node.isClickable();

        final int type = node.getInputType();
        this.numeric = type == InputType.TYPE_NUMBER_FLAG_DECIMAL || type == InputType.TYPE_CLASS_NUMBER || type == InputType.TYPE_NUMBER_FLAG_DECIMAL + InputType.TYPE_CLASS_NUMBER;

        CharSequence charSeq = node.getText();
        if(charSeq != null){
            attributes.put("text", String.valueOf(charSeq));
        }

        charSeq = node.getContentDescription();
        if(charSeq != null){
            attributes.put("description", String.valueOf(charSeq));
        }

        if (Build.VERSION.SDK_INT >= 28) {
            charSeq = node.getTooltipText();
            if (charSeq != null) {
                attributes.put("tooltip", String.valueOf(charSeq));
            }

            charSeq = node.getHintText();
            if(charSeq != null){
                attributes.put("hintText", String.valueOf(charSeq));
            }

            charSeq = node.getPaneTitle();
            if(charSeq != null){
                attributes.put("panelTitle", String.valueOf(charSeq));
            }
        }

        attributes.put("numeric", numeric + "");
        attributes.put("enabled", node.isEnabled() + "");
        attributes.put("selected", node.isSelected() + "");
        attributes.put("editable", node.isEditable() + "");
        attributes.put("checkable", node.isCheckable() + "");
        if(node.isCheckable()){
            attributes.put("checked", node.isChecked() + "");
        }

        data = node.getViewIdResourceName();
        if(data != null){
            final int idx = data.indexOf(":id/");
            if(idx > -1){
                data = data.substring(idx + 4);
            }
            attributes.put("viewId", data);
        }

        loadBounds(node);
        loadChildren(node);
    }

    @Override
    public String getViewId(){
        return attributes.get("viewId");
    }

    @Override
    public JSONObject getJsonObject(){
        JSONObject base = super.getJsonObject();
        try{
            base.put("attributes", new JSONObject(attributes));
        } catch (JSONException e) {}
        return base;
    }
}