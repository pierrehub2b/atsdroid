package com.ats.atsdroid;

import android.support.test.InstrumentationRegistry;

import com.ats.atsdroid.server.AtsWebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class AtsRunnerUsb extends AtsRunner {

    private AtsWebSocketServer tcpServer;
    // private AtsWebSocketServer udpServer;

    /* A REFACTO : NECESSAIRE POUR LE MODE UDP USB */
    public int udpPort = DEFAULT_PORT;

    @Override
    public void stop() {
        try {
            tcpServer.stop();
            // udpServer.stop();
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

        /* A REFACTO : NECESSAIRE POUR LE MODE UDP USB */
        try {
            udpPort = Integer.parseInt(InstrumentationRegistry.getArguments().getString("udpPort"));
        }catch(Exception e){}

        tcpServer = new AtsWebSocketServer(new InetSocketAddress(port), automation);
        tcpServer.start();

        // udpServer = new AtsWebSocketServer(new InetSocketAddress(port+1), automation);
        // udpServer.start();

        while (running) { }
    }
}
