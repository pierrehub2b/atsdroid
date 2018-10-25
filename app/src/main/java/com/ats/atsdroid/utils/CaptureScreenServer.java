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

    private static final int PACKET_SIZE = 504;

    private boolean running = true;
    private byte[] receiveData = new byte[1];
    private DatagramSocket serverSocket;
    private AtsAutomation automation;

    private int port = 0;

    public CaptureScreenServer(AtsAutomation automation) {
        this.automation = automation;
        try {
            serverSocket = new DatagramSocket();
            port = serverSocket.getLocalPort();
        } catch (SocketException e) {}
    }

    @Override
    public void run() {
        while(running)
        {
            try {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, 1);
                serverSocket.receive(receivePacket);

                byte[] screen = automation.getScreenData();

                int packetSize = PACKET_SIZE;
                int dataLength = screen.length;
                int currentPos = 0;

                sendData(screen, currentPos, getData(dataLength, packetSize), packetSize, receivePacket.getAddress(), receivePacket.getPort());

                while(dataLength > 0) {
                    currentPos += packetSize;
                    dataLength -= packetSize;
                    if(dataLength < packetSize) {
                        packetSize = dataLength;
                    }
                    sendData(screen, currentPos, getData(dataLength, packetSize), packetSize, receivePacket.getAddress(), receivePacket.getPort());
                }

            } catch (Exception e) {}
        }
        serverSocket.close();
    }

    private void sendData(byte[] screen, int currentPos, byte[] send, int packetSize, InetAddress address, int port) throws IOException{
        System.arraycopy(screen, currentPos, send, 4, packetSize);
        DatagramPacket packet = new DatagramPacket(send, send.length, address, port);
        serverSocket.send(packet);
    }

    private byte[] getData(int dataLength, int packetSize){
        byte[] data = new byte[packetSize + 4];
        data[0] = (byte)(dataLength >>> 24);
        data[1] = (byte)(dataLength >>> 16);
        data[2] = (byte)(dataLength >>> 8);
        data[3] = (byte)(dataLength);
        return data;
    }

    public void stop() {
        this.running = false;
    }

    public int getPort() {
        return port;
    }
}