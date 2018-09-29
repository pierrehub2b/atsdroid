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

import android.graphics.Rect;
import android.support.test.uiautomator.UiObject2;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class AtsElement implements IAtsElement {

    private static Pattern resIdPattern = Pattern.compile("(.*):id/");

    private String id;
    private UiObject2 element;
    private AtsAutomation automation;
    private Rect bounds;

    public AtsElement(AtsAutomation automation, UiObject2 element){
        this.id = UUID.randomUUID().toString();
        this.automation = automation;
        this.element = element;
        this.bounds = element.getVisibleBounds();
    }

    public String getId() {
        return id;
    }

    public List<AtsElement> getParents(){

        List<AtsElement> list = new ArrayList<AtsElement>();

        UiObject2 parent = element.getParent();
        while(parent != null){
            list.add(new AtsElement(automation, parent));
            parent = parent.getParent();
        }
        return list;
    }

    public List<UiObject2> getChildren(String tag){
        return element.findObjects(automation.getSelector(tag));
    }

    public String inputText(String value){
        element.clear();
        element.setText(value);

        return element.getText();
    }

    public void click(){
        automation.clickAtRect(bounds);
    }

    public JSONObject toJson(){

        JSONObject jsonObject = new JSONObject();

        try{
            jsonObject.put("id", id);
            jsonObject.put("rec", bounds.flattenToString().replace(" ", ","));

            String className = element.getClassName();
            jsonObject.put("tag", className.substring(className.lastIndexOf(".") + 1));

            String id = element.getResourceName();
            if(id != null){
                int idx = id.indexOf(":id/");
                if(idx > -1){
                    id = id.substring(idx + 4);
                }
                jsonObject.put("resId", id);
            }

            String text = element.getText();
            if (text != null) {
                jsonObject.put("text", text);
            }

            if (element.isCheckable()) {
                jsonObject.put("checked", element.isChecked());
            }
        } catch (JSONException e) {}

        return jsonObject;
    }
}