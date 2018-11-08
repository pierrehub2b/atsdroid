package com.ats.atsdroid.server;

import android.util.Base64;

import com.ats.atsdroid.AtsRunner;
import com.ats.atsdroid.element.AbstractAtsElement;
import com.ats.atsdroid.utils.ApplicationInfo;
import com.ats.atsdroid.utils.AtsAutomation;
import com.ats.atsdroid.utils.DeviceInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
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

            if(input != null){

                RequestType req = new RequestType(input);

                if(RequestType.APP.equals(req.type)){
                    if(req.parameters.length > 1) {
                        if(RequestType.START.equals(req.parameters[0])){
                            try {
                                ApplicationInfo app = automation.startChannel(req.parameters[1]);

                                if (app != null) {
                                    obj.put("status", "0");
                                    obj.put("message", "start app : " + app.getPackageName());
                                    obj.put("label", app.getLabel());
                                    obj.put("icon", app.getIcon());
                                } else {
                                    obj.put("status", "-5");
                                    obj.put("message", "app package not found : " + req.parameters[1]);
                                }
                            }catch (Exception e){
                                System.err.println("Ats error : " + e.getMessage());
                            }

                        }else if(RequestType.STOP.equals(req.parameters[0])){
                            automation.stopChannel(req.parameters[1]);
                            obj.put("status", "0");
                            obj.put("message", "stop app : " + req.parameters[1]);
                        }else if(RequestType.SWITCH.equals(req.parameters[0])){
                            automation.switchChannel(req.parameters[1]);
                            obj.put("status", "0");
                            obj.put("message", "switch app : " + req.parameters[1]);
                        }else if(RequestType.INFO.equals(req.parameters[0])){
                            ApplicationInfo app = automation.getApplicationInfo(req.parameters[1]);
                            if (app != null) {
                                obj.put("status", "0");
                                obj.put("info", app.getJson());
                            } else {
                                obj.put("status", "-8");
                                obj.put("message", "app not found : " + req.parameters[1]);
                            }
                        }
                    }

                }else if(RequestType.INFO.equals(req.type)){

                    try {
                        obj.put("status", "0");
                        obj.put("message", "device capabilities");
                        obj.put("id", DeviceInfo.getInstance().getDeviceId());
                        obj.put("model", DeviceInfo.getInstance().getModel());
                        obj.put("manufacturer", DeviceInfo.getInstance().getManufacturer());
                        obj.put("brand", DeviceInfo.getInstance().getBrand());
                        obj.put("version", DeviceInfo.getInstance().getVersion());
                        obj.put("os", "android");
                        obj.put("driverVersion", "1.0.0");
                        obj.put("systemName", DeviceInfo.getInstance().getSystemName());
                        obj.put("deviceWidth", DeviceInfo.getInstance().getDeviceWidth());
                        obj.put("deviceHeight", DeviceInfo.getInstance().getDeviceHeight());
                        obj.put("channelWidth", automation.getChannelWidth());
                        obj.put("channelHeight", automation.getChannelHeight());
                        obj.put("channelX", automation.getChannelX());
                        obj.put("channelY", automation.getChannelY());
                        obj.put("bluetoothName", DeviceInfo.getInstance().getBtAdapter());

                        List<ApplicationInfo> apps = automation.getApplications();

                        JSONArray applications = new JSONArray();
                        for (ApplicationInfo appInfo : apps) {
                            applications.put(appInfo.getJson());
                        }
                        obj.put("applications", applications);

                    }catch (Exception e){
                        obj.put("status", "-99");
                        obj.put("message", e.getMessage());
                    }

                }else if(RequestType.DRIVER.equals(req.type)){
                    if(req.parameters.length > 0) {
                        if(RequestType.START.equals(req.parameters[0])){

                            automation.startDriverThread();

                            obj.put("status", "0");
                            obj.put("os", "android");
                            obj.put("driverVersion", "1.0.0");
                            obj.put("systemName", DeviceInfo.getInstance().getSystemName());
                            obj.put("deviceWidth", DeviceInfo.getInstance().getDeviceWidth());
                            obj.put("deviceHeight", DeviceInfo.getInstance().getDeviceHeight());
                            obj.put("channelWidth", automation.getChannelWidth());
                            obj.put("channelHeight", automation.getChannelHeight());
                            obj.put("channelX", automation.getChannelX());
                            obj.put("channelY", automation.getChannelY());
                            obj.put("screenCapturePort", automation.getScreenCapturePort());

                        }else if(RequestType.STOP.equals(req.parameters[0])){

                            automation.stopDriverThread();
                            obj.put("status", "0");
                            obj.put("message", "stop ats driver");

                        }else if(RequestType.QUIT.equals(req.parameters[0])){

                            automation.stopDriverThread();
                            obj.put("status", "0");
                            obj.put("message", "close ats driver");

                            sendResponseData(obj);

                            runner.setRunning(false);
                            automation.terminate();

                            return;

                        }else{
                            obj.put("status", "-1");
                            obj.put("message", "wrong driver action type : " + req.parameters[0]);
                        }
                    }else{
                        obj.put("status", "-1");
                        obj.put("message", "missing driver action");
                    }

                }else if(RequestType.BUTTON.equals(req.type)){
                    if(req.parameters.length > 0) {
                        automation.deviceButton(req.parameters[0]);
                        obj.put("status", "0");
                        obj.put("message", "button : " + req.parameters[0]);
                    }else{
                        obj.put("status", "-1");
                        obj.put("message", "missing button type");
                    }

                }else if(RequestType.CAPTURE.equals(req.type)){

                    if(req.parameters.length > 0 && "reload".equals(req.parameters[0])){
                        automation.reloadRoot();
                    }
                    obj = automation.getRootObject();

                }else if(RequestType.ELEMENT.equals(req.type)){

                    if(req.parameters.length > 2) {
                        AbstractAtsElement element = automation.getElement(req.parameters[0]);
                        if(element != null){

                            if(RequestType.INPUT.equals(req.parameters[1])){
                                String text = "";

                                    try {
                                        byte[] data = Base64.decode(req.parameters[2], Base64.DEFAULT);
                                        text = new String(data, StandardCharsets.UTF_8);
                                    }catch (Exception e){
                                        obj.put("error", e.getMessage());
                                    }

                                element.inputText(automation, text);

                                obj.put("status", "0");
                                obj.put("message", "element send keys : " + text);

                            }else{

                                int offsetX = 0;
                                int offsetY = 0;

                                if (req.parameters.length > 3){
                                    try {
                                        offsetX = Integer.parseInt(req.parameters[2]);
                                        offsetY = Integer.parseInt(req.parameters[3]);
                                    } catch (NumberFormatException e) {}
                                }

                                if(RequestType.TAP.equals(req.parameters[1])){

                                    element.click(automation, offsetX, offsetY);

                                    obj.put("status", "0");
                                    obj.put("message", "click on element");

                                }else if(RequestType.SWIPE.equals(req.parameters[1])){
                                    int directionX = 0;
                                    int directionY = 0;
                                    if (req.parameters.length > 5){
                                        try {
                                            directionX = Integer.parseInt(req.parameters[4]);
                                            directionY = Integer.parseInt(req.parameters[5]);
                                        } catch (NumberFormatException e) {}
                                    }
                                    element.swipe(automation, offsetX, offsetY, directionX, directionY);
                                    obj.put("status", "0");
                                    obj.put("message", "swipe element to " + directionX + ":" + directionY);
                                }
                            }

                        }else{
                            obj.put("status", "-1");
                            obj.put("message", "element not found");
                        }
                    }else{
                        obj.put("status", "-1");
                        obj.put("message", "missing element id");
                    }

                }else{
                    obj.put("status", "-99");
                    obj.put("message", "unknown command");
                }

                sendResponseData(obj);
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

    private void sendResponseData(JSONObject obj) throws  IOException{

        byte[] data = new byte[0];
        try{
            data = obj.toString().getBytes("UTF-8");
        }catch(UnsupportedEncodingException e){}

        byte[] header = ("HTTP/1.1 200 OK\r\nServer: AtsDroid Driver\r\nDate: " + new Date() + "\r\nContent-type: " + JSON_RESPONSE_TYPE + "\r\nContent-length: " + data.length + "\r\n\r\n").getBytes();
        BufferedOutputStream bf = new BufferedOutputStream(socket.getOutputStream());
        bf.write(header, 0, header.length);
        bf.write(data, 0, data.length);
        bf.flush();
    }
}