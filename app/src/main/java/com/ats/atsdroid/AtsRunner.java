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

package com.ats.atsdroid;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.ats.atsdroid.server.AtsHttpServer;
import com.ats.atsdroid.utils.AtsAutomation;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@RunWith(AndroidJUnit4.class)
public class AtsRunner {

    public static final int DEFAULT_PORT = 8080;

    private AtsAutomation automation;
    private boolean running = true;

    public void stop(){
        running = false;
    }

    @Test
    public void testMain() {
        Boolean usbMode = false;
        int port = DEFAULT_PORT;
        String ipAddress = "";

        try {
            port = Integer.parseInt(InstrumentationRegistry.getArguments().getString("atsPort"));
        }catch(Exception e){}

        try {
            usbMode = Boolean.parseBoolean(InstrumentationRegistry.getArguments().getString("usbMode"));
        }catch(Exception e){}

        try {
            ipAddress = InstrumentationRegistry.getArguments().getString("ipAddress");
        }catch(Exception e){}

        automation = new AtsAutomation(port, this, ipAddress, usbMode);

        try {
            if(!usbMode) {
                ServerSocket serverConnect = new ServerSocket(port);
                while (running) {
                    final AtsHttpServer atsServer = new AtsHttpServer(serverConnect.accept(), automation);
                    (new Thread(atsServer)).start();
                }
            }
        } catch(IOException e){
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }
}