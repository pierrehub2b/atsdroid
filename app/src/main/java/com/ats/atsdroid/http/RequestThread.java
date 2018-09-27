package com.ats.atsdroid.http;

import com.ats.atsdroid.AtsRunner;
import com.ats.atsdroid.automation.AtsAutomation;

import java.io.IOException;
import java.net.ServerSocket;

public class RequestThread extends Thread {

    private AtsRunner runner;
    private int serverPort;
    private float screenPicScale;
    private int quality;

    public RequestThread(AtsRunner runner, int serverPort, float screenPicScale, int quality){
        this.runner = runner;
        this.serverPort = serverPort;
        this.screenPicScale = screenPicScale;
        this.quality = quality;
    }

    @Override
    public void run() {

        AtsAutomation automation = new AtsAutomation(screenPicScale, quality);

        try {
            ServerSocket httpServerSocket = new ServerSocket(serverPort);
            ResponseThread httpResponseThread;

            while(runner.isRunning()){
                httpResponseThread =
                        new ResponseThread(
                                httpServerSocket.accept(),
                                runner,
                                automation
                                );
                httpResponseThread.start();
            }
            httpServerSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}