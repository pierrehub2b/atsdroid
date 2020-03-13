package com.ats.atsdroid;

import com.ats.atsdroid.server.AtsWebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class AtsRunnerUsb extends AtsRunner {

    private AtsWebSocketServer server;

    @Override
    public void stop() {
        try {
            server.stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        super.stop();
    }

    @Override
    public void testMain() {
        super.testMain();

        server = new AtsWebSocketServer(new InetSocketAddress(port), automation);
        Thread thread = new Thread(server);
        thread.start();

        while (running) { }
    }
}
