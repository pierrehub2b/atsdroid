package com.ats.atsdroid.http;

import android.graphics.Rect;
import android.support.test.uiautomator.StaleObjectException;
import android.support.test.uiautomator.UiObject2;

import com.ats.atsdroid.AtsRunner;
import com.ats.atsdroid.DeviceInfo;
import com.ats.atsdroid.automation.AtsAutomation;

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
import java.nio.charset.StandardCharsets;
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
            os = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1), true);
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

        JSONObject obj = new JSONObject();
        RequestType req = new RequestType(request);

        try {
            switch (req.type){

                case RequestType.STARTACTIVITY :

                    String startActivity = automation.startActivity("sport.android.betclic.fr", "md5a690acf0a0c74eeffb5a713ac30d2f9d.SplashScreenActivity");

                    obj.put("status", "0");
                    obj.put("message", "activity started");
                    obj.put("name", startActivity);

                    break;

                case RequestType.STOP:

                    String stopActivity = automation.stopActivity();

                    obj.put("status", "0");
                    obj.put("message", "stop activity");
                    if(stopActivity != null){
                        obj.put("name", stopActivity);
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

                    List<UiObject2> list = automation.getAllElements();
                    JSONArray jsonArray = new JSONArray();

                    for(int i=0; i<list.size(); i++){

                        try {
                            JSONObject element = new JSONObject();

                            UiObject2 uiObj = list.get(i);

                            element.put("resId", uiObj.getResourceName().substring(automation.getPackageLength()));

                            String className = uiObj.getClassName();
                            element.put("tag", className.substring(className.lastIndexOf(".") + 1));

                            String text = uiObj.getText();
                            if (text != null) {
                                element.put("text", text);
                            }

                            if (uiObj.isCheckable()) {
                                element.put("checked", uiObj.isChecked());
                            }

                            element.put("rec", uiObj.getVisibleBounds().flattenToString().replace(" ", ","));

                            jsonArray.put(element);

                        }catch(StaleObjectException e){}
                    }

                    obj.put("status", "0");
                    obj.put("elements", jsonArray);

                    break;

                case RequestType.MOUSECLICK:

                    break;

                case RequestType.SETTEXT:

                    UiObject2 found = automation.getElementByRes("sport.android.betclic.fr:id/login_username");
                    found.clear();
                    if(req.parameters.length > 0){
                        try {
                            found.setText(URLDecoder.decode(req.parameters[0], "utf-8"));
                        }catch(UnsupportedEncodingException e){}
                    }
                    obj.put("status", "0");
                    obj.put("message", found.getText());

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
                    obj.put("message", "capabilities");
                    obj.put("systemName", DeviceInfo.getInstance().getSystemName());
                    obj.put("displayWidth", DeviceInfo.getInstance().getDisplayWidth());
                    obj.put("displayHeight", DeviceInfo.getInstance().getDisplayHeight());
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