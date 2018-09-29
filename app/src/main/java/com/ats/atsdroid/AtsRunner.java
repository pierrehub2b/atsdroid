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

import android.support.test.runner.AndroidJUnit4;

import com.ats.atsdroid.http.RequestThread;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@RunWith(AndroidJUnit4.class)
public class AtsRunner {

    private static final int serverPort = 8080;

    private boolean running = true;
    public boolean isRunning(){
        return running;
    }
    public void setRunning(boolean value){
        this.running = value;
    }

    @Test
    public void testMain() {

        RequestThread httpServerThread = new RequestThread(this, serverPort);
        httpServerThread.start();

        while(running){}

        httpServerThread.interrupt();
    }
}