package com.ats.atsdroid;

import android.support.test.InstrumentationRegistry;

import com.ats.atsdroid.server.AtsWebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

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
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        super.stop();
    }

    @Override
    public void testMain() {
        super.testMain();

        try {
            /* A REFACTO : NECESSAIRE POUR LE MODE UDP USB */
            udpPort = Integer.parseInt(InstrumentationRegistry.getArguments().getString("udpPort"));

            ServerSocket serverSocket = new ServerSocket(0);
            int availablePort = serverSocket.getLocalPort();
            serverSocket.close();

            tcpServer = new AtsWebSocketServer(new InetSocketAddress(availablePort), automation);
            tcpServer.start();

            // udpServer = new AtsWebSocketServer(new InetSocketAddress(port+1), automation);
            // udpServer.start();

            while (running) { }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
