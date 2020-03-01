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

package com.ats.atsdroid.ui;

import android.app.Activity;
import android.app.KeyguardManager;
import android.os.Bundle;
import android.view.WindowManager;

public class AtsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(new AtsView(this));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();
    }

    /*@Override
    public void dump (String prefix, FileDescriptor fd, PrintWriter writer, String[] args){
        try {
            if(args.length > 0){
                String type = args[0];
                String[] parameters = new String[0];

                if(args.length > 1) {
                    parameters = Arrays.copyOfRange(args, 1, args.length);
                }

                final AtsResponse resp = automation.executeRequest(new RequestType(type, parameters), true);
                resp.sendDataToUsbPort(writer);

            }else{
                writer.print("no enough args");
            }
        } catch(Exception ex) {
            String error = ex.getMessage();
        } finally {
            writer.flush();
        }
    }*/
}