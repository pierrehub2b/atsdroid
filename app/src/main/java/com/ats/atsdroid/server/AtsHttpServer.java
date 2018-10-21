package com.ats.atsdroid.server;

import android.util.Base64;

import com.ats.atsdroid.AtsRunner;
import com.ats.atsdroid.element.AbstractAtsElement;
import com.ats.atsdroid.element.AtsElement;
import com.ats.atsdroid.utils.AtsAutomation;
import com.ats.atsdroid.utils.ApplicationInfo;
import com.ats.atsdroid.utils.DeviceInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class AtsHttpServer implements Runnable{

    private final static String JSON_RESPONSE_TYPE = "application/json";

    private Socket socket;
    private AtsRunner runner;
    private AtsAutomation automation;

    public AtsHttpServer(Socket socket, AtsRunner runner, AtsAutomation automation) {
        this.socket = socket;
        this.runner = runner;
        this.automation = automation;
    }

    @Override
    public void run() {

        BufferedReader in = null;
        BufferedOutputStream out = null;

        try {

            out = new BufferedOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String input = in.readLine();
            JSONObject obj = new JSONObject();

            if(input == null){
                sendResponseData(JSON_RESPONSE_TYPE, getJsonData(obj));
            }else{
                RequestType req = new RequestType(input);

                switch (req.type){

                    case RequestType.CHANNEL:

                        if(req.parameters.length > 1) {
                            if(RequestType.START.equals(req.parameters[0])){
                                ApplicationInfo app = automation.startChannel(req.parameters[1]);

                                if(app != null){
                                    obj.put("status", "0");
                                    obj.put("message", "start channel : " + app.getPackageName());
                                    obj.put("label", app.getLabel());
                                    obj.put("icon", app.getIcon());
                                }else{
                                    obj.put("status", "-5");
                                    obj.put("message", "application package not found : " + req.parameters[1]);
                                }

                            }else if(RequestType.STOP.equals(req.parameters[0])){
                                automation.stopChannel(req.parameters[1]);
                                obj.put("status", "0");
                                obj.put("message", "stop channel : " + req.parameters[1]);
                            }else if(RequestType.SWITCH.equals(req.parameters[0])){
                                automation.switchChannel(req.parameters[1]);
                                obj.put("status", "0");
                                obj.put("message", "switch channel : " + req.parameters[1]);
                            }
                        }

                        sendResponseData(JSON_RESPONSE_TYPE, getJsonData(obj));
                        break;

                    case RequestType.APPLICATIONS :

                        obj.put("message", "installed applications");
                        List<ApplicationInfo> apps = automation.getApplications();

                        JSONObject applications = new JSONObject();
                        for(ApplicationInfo appInfo : apps){
                            JSONObject appDetails = new JSONObject();
                            appDetails.put("activity", appInfo.getActivity(0));
                            appDetails.put("label", appInfo.getLabel());
                            applications.put(appInfo.getPackageName(), appDetails);
                        }
                        obj.put("applications", applications);

                        sendResponseData(JSON_RESPONSE_TYPE, getJsonData(obj));
                        break;

                    case RequestType.DRIVER :

                        if(req.parameters.length > 0) {
                            if(RequestType.START.equals(req.parameters[0])){

                                int quality = 2;
                                if(req.parameters.length > 1) {
                                    try {
                                        quality = Integer.parseInt(req.parameters[1]);
                                    } catch (NumberFormatException e) {}
                                }
                                automation.setQuality(quality);
                                automation.deviceWakeUp();

                                obj.put("status", "0");
                                obj.put("message", "start ats driver with image quality = " + quality);
                                obj.put("os", "android");
                                obj.put("driverVersion", "1.0.0");
                                obj.put("systemName", DeviceInfo.getInstance().getSystemName());
                                obj.put("deviceWidth", DeviceInfo.getInstance().getDeviceWidth());
                                obj.put("deviceHeight", DeviceInfo.getInstance().getDeviceHeight());
                                obj.put("channelWidth", automation.getChannelWidth());
                                obj.put("channelHeight", automation.getChannelHeight());
                                obj.put("channelX", automation.getChannelX());
                                obj.put("channelY", automation.getChannelY());
                                obj.put("id", DeviceInfo.getInstance().getDeviceId());
                                obj.put("model", DeviceInfo.getInstance().getModel());
                                obj.put("manufacturer", DeviceInfo.getInstance().getManufacturer());
                                obj.put("brand", DeviceInfo.getInstance().getBrand());
                                obj.put("version", DeviceInfo.getInstance().getVersion());
                                obj.put("host", DeviceInfo.getInstance().getHostName());
                                obj.put("bluetoothName", DeviceInfo.getInstance().getBtAdapter());

                            }else if(RequestType.STOP.equals(req.parameters[0])){

                                automation.deviceSleep();
                                obj.put("status", "0");
                                obj.put("message", "stop ats driver");

                            }else if(RequestType.QUIT.equals(req.parameters[0])){

                                automation.deviceSleep();
                                obj.put("status", "0");
                                obj.put("message", "close ats driver");

                                sendResponseData(JSON_RESPONSE_TYPE, getJsonData(obj));

                                runner.setRunning(false);
                                automation.terminate();

                            }else{
                                obj.put("status", "-1");
                                obj.put("message", "wrong driver action type : " + req.parameters[0]);
                            }
                        }else{
                            obj.put("status", "-1");
                            obj.put("message", "missing driver action");
                        }

                        sendResponseData(JSON_RESPONSE_TYPE, getJsonData(obj));
                        break;

                    case RequestType.BUTTON:

                        if(req.parameters.length > 0) {
                            automation.deviceButton(req.parameters[0]);
                            obj.put("status", "0");
                            obj.put("message", "button : " + req.parameters[0]);
                        }else{
                            obj.put("status", "-1");
                            obj.put("message", "missing button type");
                        }

                        sendResponseData(JSON_RESPONSE_TYPE, getJsonData(obj));
                        break;

                    case RequestType.CAPTURE:

                        if(req.parameters.length > 0 && "elements".equals(req.parameters[0])){
                            if(req.parameters.length > 1 && "reload".equals(req.parameters[1])){
                                automation.reloadRoot();
                            }
                            sendResponseData("application/json", automation.getRootObject().toString().getBytes());
                        }else{
                            try {
                                sendResponseData("image/" + automation.getImageType(), automation.getScreenData());
                            }catch(Exception e){
                                sendResponseData("text/html", e.getMessage().getBytes());
                            }
                        }
                        break;

                    case RequestType.TAP:

                        if(req.parameters.length > 0) {

                            String elementId = req.parameters[0];
                            int offsetX = 0;
                            int offsetY = 0;

                            if (req.parameters.length > 2){
                                try {
                                    offsetX = Integer.parseInt(req.parameters[1]);
                                    offsetY = Integer.parseInt(req.parameters[2]);
                                } catch (NumberFormatException e) {}
                            }

                            AbstractAtsElement element = automation.getElement(elementId);
                            if(element != null){

                                element.click(automation, offsetX, offsetY);

                                obj.put("status", "0");
                                obj.put("message", "click on element : " + elementId);
                            }else{
                                obj.put("status", "-9");
                                obj.put("message", "element does not exists");
                            }

                        }else{
                            obj.put("status", "-1");
                            obj.put("message", "missing element ats id");
                        }

                        sendResponseData(JSON_RESPONSE_TYPE, getJsonData(obj));
                        break;

                    case RequestType.INPUT:

                        if(req.parameters.length > 0){

                            String elementId = req.parameters[0];
                            AbstractAtsElement element = automation.getElement(elementId);

                            if(element != null){

                                String text = "";
                                if(req.parameters.length > 1){
                                    try {
                                        byte[] data = Base64.decode(req.parameters[1], Base64.DEFAULT);
                                        text = new String(data, StandardCharsets.UTF_8);
                                    }catch (Exception e){
                                        obj.put("error", e.getMessage());
                                    }
                                }

                                element.inputText(automation, text);

                                obj.put("status", "0");
                                obj.put("message", "set element : " + element.getId() + " text = " + text);
                            }else{
                                obj.put("status", "-9");
                                obj.put("message", "element does not exists");
                            }

                        }else{
                            obj.put("status", "-1");
                            obj.put("message", "missing element ats id");
                        }

                        sendResponseData(JSON_RESPONSE_TYPE, getJsonData(obj));
                        break;

                    default:
                        obj.put("status", "-99");
                        obj.put("message", "unknown command");

                        sendResponseData(JSON_RESPONSE_TYPE, getJsonData(obj));
                        break;
                }
            }

        } catch (IOException | JSONException e) {

        } finally {
            try {
                in.close();
                out.close();
                socket.close(); // we close socket connection
            } catch (Exception e) {}
        }
    }

    private byte[] getJsonData(JSONObject obj){
        try{
            return obj.toString().getBytes("ISO-8859-1");
        }catch(UnsupportedEncodingException e){
            return new byte[0];
        }
    }

    private void sendResponseData(String type, byte[] data) throws  IOException{
        byte[] header = ("HTTP/1.1 200 OK\r\nServer: AtsDroid Driver\r\nDate: " + new Date() + "\r\nContent-type: " + type + "\r\nContent-length: " + data.length + "\r\n\r\n").getBytes();
        BufferedOutputStream bf = new BufferedOutputStream(socket.getOutputStream());
        bf.write(header, 0, header.length);
        bf.write(data, 0, data.length);
        bf.flush();
    }
}