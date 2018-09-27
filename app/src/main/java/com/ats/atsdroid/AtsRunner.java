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

    static final int serverPort = 8080;
    static final float screenPicScale = 0.70F;
    static final int screenPicQuality = 40;

    private boolean running = true;
    public boolean isRunning(){
        return running;
    }

    public void setRunning(boolean value){
        this.running = value;
    }

    @Test
    public void testMain() {

        RequestThread httpServerThread = new RequestThread(this, serverPort, screenPicScale, screenPicQuality);
        httpServerThread.start();

        while(running){

        }
    }
}