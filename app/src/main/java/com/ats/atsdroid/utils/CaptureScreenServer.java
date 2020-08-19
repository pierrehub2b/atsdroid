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

package com.ats.atsdroid.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class CaptureScreenServer implements Runnable  {

    private static final int PACKET_SIZE = 1430;

    private boolean running = true;
    private final byte[] receiveData = new byte[1];
    private DatagramSocket serverSocket;
    private final AtsAutomation automation;

    private int port = 0;

    public CaptureScreenServer(AtsAutomation automation) {
        this.automation = automation;
        try {
            serverSocket = new DatagramSocket();
            port = serverSocket.getLocalPort();
            AtsAutomation.sendLogs("Setup UDP port: " + port + "\n");
        } catch (SocketException e) {
            AtsAutomation.sendLogs("Error socket exception:" + e.getMessage() + "\n");
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while(running)
        {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, 1);
                serverSocket.receive(receivePacket);

                final int port = receivePacket.getPort();
                final InetAddress address = receivePacket.getAddress();

                final byte[] screen = automation.getScreenData();
                int dataLength = screen.length;

                int packetSize = PACKET_SIZE;
                int currentPos = 0;

                sendData(screen, currentPos, dataLength, packetSize, address, port);

                while (dataLength > 0) {
                    currentPos += packetSize;
                    dataLength -= packetSize;
                    if (dataLength < packetSize) {
                        packetSize = dataLength;
                    }
                    sendData(screen, currentPos, dataLength, packetSize, address, port);
                }
            } catch (Exception e) {
                AtsAutomation.sendLogs("Error on screen server:" + e.getMessage() + "\n");
            }
        }
        serverSocket.close();
    }

    private void sendData(byte[] screen, int currentPos, int dataLength, int packetSize, InetAddress address, int port) throws IOException{

        final byte[] send = getData(currentPos, dataLength, packetSize);
        System.arraycopy(screen, currentPos, send, 8, packetSize);
        serverSocket.send(new DatagramPacket(send, send.length, address, port));
    }

    private byte[] getData(int dataPos, int dataLength, int packetSize){
        final byte[] data = new byte[packetSize + 8];

        data[0] = (byte)(dataPos >>> 24);
        data[1] = (byte)(dataPos >>> 16);
        data[2] = (byte)(dataPos >>> 8);
        data[3] = (byte)(dataPos);

        data[4] = (byte)(dataLength >>> 24);
        data[5] = (byte)(dataLength >>> 16);
        data[6] = (byte)(dataLength >>> 8);
        data[7] = (byte)(dataLength);

        return data;
    }
    public int getPort() {
        return port;
    }
}