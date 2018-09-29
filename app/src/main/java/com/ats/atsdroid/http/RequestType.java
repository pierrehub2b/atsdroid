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

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestType {

    public static final String STARTCHANNEL = "start_channel";
    public static final String STOPCHANNEL = "stop_channel";
    public static final String ELEMENTS = "elements";
    public static final String PARENTS = "parents";
    public static final String SCREENSHOT = "screen_shot";
    public static final String SETTEXT = "set_text";
    public static final String EXIT = "exit";
    public static final String MOUSECLICK = "mouse_click";
    public static final String CAPABILITIES = "capabilities";

    private static Pattern requestPattern = Pattern.compile("GET /(.*) HTTP/1.1");

    public String type;
    public String[] parameters = new String[0];

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
