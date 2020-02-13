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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestType {

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

    public RequestType(String value, String body, String userAgent){

        this.userAgent = userAgent;

        Matcher match = requestPattern.matcher(value);
        if(match.find()){
            this.type = match.group(1);
            this.parameters = body.split("\n");
        }
    }

    public RequestType(String value, String[] args){
        this.type = value;
        this.parameters = args;
    }
}
