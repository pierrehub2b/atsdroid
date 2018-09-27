package com.ats.atsdroid.http;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestType {

    public static final String CAPABILITIES = "capabilities";
    public static final String ELEMENTS = "elements";
    public static final String SCREENSHOT = "screen_shot";
    public static final String SETTEXT = "set_text";
    public static final String EXIT = "exit";
    public static final String MOUSECLICK = "mouse_click";
    public static final String STARTACTIVITY = "start_activity";
    public static final String STOP = "stop";

    private static Pattern requestPattern = Pattern.compile("GET /(.*) HTTP/1.1");

    public String type;
    public String[] parameters;

    public RequestType(String value){
        Matcher match = requestPattern.matcher(value);
        if(match.find()){
            String data = match.group(1);
            String[] params = data.split("/");
            if(params.length > 0){
                this.type = params[0];
                if(params.length > 1){
                    this.parameters = Arrays.copyOfRange(params, 1, params.length);
                }
            }
        }
    }
}
