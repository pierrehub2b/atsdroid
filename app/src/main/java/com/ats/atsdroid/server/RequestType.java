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

package com.ats.atsdroid.server;

import android.util.Log;
import com.ats.atsdroid.utils.AtsAutomation;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestType {

    private final static String CONTENT_LENGTH = "Content-Length: ";
    private final static String USER_AGENT = "User-Agent: ";

    public static final String APP = "app";
    public static final String DRIVER = "driver";
    public static final String QUIT = "quit";
    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String SWITCH = "switch";
    public static final String BUTTON = "button";
    public static final String INFO = "info";
    public static final String PACKAGE = "package";
    public static final String CAPTURE = "capture";
    public static final String ELEMENT = "element";
    public static final String INPUT = "input";
    public static final String TAP = "tap";
    public static final String SWIPE = "swipe";
    public static final String SCREENSHOT = "screenshot";
    public static final String SCREENSHOT_HIRES = "hires";

    private static Pattern requestPattern = Pattern.compile("POST /(.*) HTTP/1.1");

    public String type = "";
    public String[] parameters = new String[0];
    public String userAgent = "";

    public RequestType(String value, String body, String userAgent) {

        this.userAgent = userAgent;

        Matcher match = requestPattern.matcher(value);
        if(match.find()){
            this.type = match.group(1);
            this.parameters = body.split("\n");
        }
    }

    /* public RequestType(String value, String[] args){
        this.type = value;
        this.parameters = args;
    } */


    public static RequestType generate(BufferedReader in, String userAgent) throws IOException {
        String line;
        int contentLength = 0;


        String input = in.readLine();

        while (!(line = in.readLine()).equals("")) {
            if (line.startsWith(CONTENT_LENGTH)) {
                try {
                    contentLength = Integer.parseInt(line.substring(CONTENT_LENGTH.length()));
                } catch(NumberFormatException e){
                    AtsAutomation.sendLogs("Error number format expression on HttpServer:" + e.getMessage() + "\n");
                }
            } else if(line.startsWith(USER_AGENT)){
                userAgent = line.substring(USER_AGENT.length()) + " " + userAgent;
            }
        }

        String postData = "";
        if (contentLength > 0) {
            char[] charArray = new char[contentLength];
            in.read(charArray, 0, contentLength);
            postData = new String(charArray);
        }

        if (input != null) {
            return new RequestType(input, postData, userAgent);
        } else {
            return null;
        }
    }
}
