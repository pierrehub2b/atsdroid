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

package com.ats.atsdroid.http;

import android.support.test.uiautomator.StaleObjectException;
import android.support.test.uiautomator.UiObject2;

import com.ats.atsdroid.AtsRunner;
import com.ats.atsdroid.DeviceInfo;
import com.ats.atsdroid.automation.AtsAutomation;
import com.ats.atsdroid.automation.AtsElement;
import com.ats.atsdroid.automation.AtsElementRoot;
import com.ats.atsdroid.automation.IAtsElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;

public class ResponseThread extends Thread {

    private Socket socket;
    private AtsRunner runner;
    private AtsAutomation automation;

    public ResponseThread(Socket socket, AtsRunner runner, AtsAutomation automation){
        this.socket = socket;
        this.runner = runner;
        this.automation = automation;
    }

    @Override
    public void run() {

        BufferedReader is;
        PrintWriter os;

        try {
            os = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("ISO-8859-1")), true);
            is = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String request = is.readLine();

            if(request != null){
                sendResponse(os, getJsonResponse(request).toString());
            }

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return;
    }

    private JSONObject getJsonResponse(String request){

        JSONArray jsonArray = null;

        JSONObject obj = new JSONObject();
        RequestType req = new RequestType(request);

        try {
            switch (req.type){

                case RequestType.STARTCHANNEL :

                    automation.wakeUp();
                    if(req.parameters.length > 1 && req.parameters[0].length() > 0 && req.parameters[1].length() > 0){

                        int quality = 2;
                        if(req.parameters.length > 2) {
                            try {
                                quality = Integer.parseInt(req.parameters[2]);
                            } catch (NumberFormatException e) {}
                        }
                        String startActivity = automation.startActivity(quality, req.parameters[0], req.parameters[1]);

                        obj.put("status", "0");
                        obj.put("message", "activity started -> " + startActivity);

                    }else{
                        obj.put("status", "-1");
                        obj.put("message", "activity parameters error");
                    }

                    break;

                case RequestType.STOPCHANNEL:

                    String stopActivity = automation.stopActivity();
                    automation.sleep();

                    obj.put("status", "0");
                    if(stopActivity != null){
                        obj.put("message", "stop activity -> " + stopActivity);
                    }
                    break;

                case RequestType.SCREENSHOT:

                    String data = automation.getScreenPic();
                    if(data != null) {
                        obj.put("status", "0");
                        obj.put("img", data);
                    }else{
                        obj.put("status", "-1");
                        obj.put("message", "screen capture failed");
                    }
                    break;

                case RequestType.ELEMENTS:

                    IAtsElement parent = null;
                    String tag = "*";

                    if(req.parameters.length > 0 && req.parameters[0].length() > 0){
                        tag = req.parameters[0];
                        if(req.parameters.length > 1){
                            parent = automation.getCachedElement(req.parameters[1]);
                        }else{
                            automation.clearCachedElements();
                            parent = new AtsElementRoot(automation);

                        }
                    }else{
                        parent = new AtsElementRoot(automation);
                    }

                    List<UiObject2> children = parent.getChildren(tag);
                    jsonArray = new JSONArray();

                    for(int i=0; i<children.size(); i++){
                        try {
                            jsonArray.put(automation.addCachedElement(children.get(i)));
                        }catch (StaleObjectException e){}
                    }

                    obj.put("status", "0");
                    obj.put("elements", jsonArray);

                    break;

                case RequestType.PARENTS:

                    jsonArray = new JSONArray();

                    if(req.parameters.length > 0 && req.parameters[0].length() > 0) {
                        List<AtsElement> parents = automation.getCachedElement(req.parameters[0]).getParents();
                        for (int i = 0; i < parents.size(); i++) {
                            try {
                                jsonArray.put(parents.get(i).toJson());
                            } catch (StaleObjectException e) {
                            }
                        }
                    }

                    obj.put("status", "0");
                    obj.put("parents", jsonArray);

                case RequestType.MOUSECLICK:

                    obj.put("status", "0");
                    if(req.parameters.length > 0){
                        automation.getCachedElement(req.parameters[0]).click();
                    }

                    break;

                case RequestType.SETTEXT:

                    obj.put("status", "0");

                    String text = "";
                    if(req.parameters.length > 0 && req.parameters[0].length() > 0){
                        try{
                            text = URLDecoder.decode(req.parameters[0], "utf-8");
                        }catch(UnsupportedEncodingException e){}

                        if(req.parameters.length > 1){
                            obj.put("message",
                                    automation.getCachedElement(
                                            req.parameters[1]).inputText(text));
                        }
                    }

                    break;

                case RequestType.EXIT:
                    automation.stopActivity();

                    obj.put("status", "0");
                    obj.put("message", "exit ats driver");

                    runner.setRunning(false);
                    automation.terminate();

                    break;

                case RequestType.CAPABILITIES:

                    obj.put("status", "0");
                    obj.put("message", "start channel");
                    obj.put("systemName", DeviceInfo.getInstance().getSystemName());
                    obj.put("deviceWidth", DeviceInfo.getInstance().getDeviceWidth());
                    obj.put("deviceHeight", DeviceInfo.getInstance().getDeviceHeight());
                    obj.put("displayWidth", DeviceInfo.getInstance().getDisplayWidth());
                    obj.put("displayHeight", DeviceInfo.getInstance().getDisplayHeight());
                    obj.put("scaleWidth", DeviceInfo.getInstance().getWidthScale());
                    obj.put("scaleHeight", DeviceInfo.getInstance().getHeightScale());
                    obj.put("id", DeviceInfo.getInstance().getDeviceId());
                    obj.put("model", DeviceInfo.getInstance().getModel());
                    obj.put("manufacturer", DeviceInfo.getInstance().getManufacturer());
                    obj.put("brand", DeviceInfo.getInstance().getBrand());
                    obj.put("version", DeviceInfo.getInstance().getVersion());
                    obj.put("host", DeviceInfo.getInstance().getHostName());
                    obj.put("BluetoothName", DeviceInfo.getInstance().getBtAdapter());

                    break;

                default:
                    obj.put("status", "-99");
                    obj.put("message", "unknown command");
                    break;
            }
        } catch (JSONException e) {}

        return obj;
    }

    private void sendResponse(PrintWriter os, String message){
        os.print("HTTP/1.0 200" + "\r\n");
        os.print("Content type: application/json" + "\r\n");
        os.print("Content length: " + message.length() + "\r\n");
        os.print("\r\n");
        os.print(message + "\r\n");
        os.flush();
    }
}