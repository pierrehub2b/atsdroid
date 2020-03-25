package com.ats.atsdroid;

import com.ats.atsdroid.server.AtsHttpServer;
import java.io.IOException;
import java.net.ServerSocket;

public class AtsRunnerWifi extends AtsRunner {

    @Override
    public void testMain() {
        super.testMain();

        try {
            ServerSocket serverConnect = new ServerSocket(port);
            while (running) {
                final AtsHttpServer atsServer = new AtsHttpServer(serverConnect.accept(), automation);
                // create dedicated thread to manage the client connection
                Thread thread = new Thread(atsServer);
                thread.start();
            }
        } catch(IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }
}
