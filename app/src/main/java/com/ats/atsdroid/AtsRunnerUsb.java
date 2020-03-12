package com.ats.atsdroid;

import com.ats.atsdroid.server.AtsWebSocketServer;
import java.net.InetSocketAddress;

public class AtsRunnerUsb extends AtsRunner {

    @Override
    public void testMain() {
        super.testMain();

        AtsWebSocketServer server = new AtsWebSocketServer(new InetSocketAddress(port), automation);
        Thread thread = new Thread(server);
        thread.start();

        while (running) { }
    }
}
