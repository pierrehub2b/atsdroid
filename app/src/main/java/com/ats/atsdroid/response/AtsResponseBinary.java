package com.ats.atsdroid.response;

import com.ats.atsdroid.utils.AtsAutomation;

import org.java_websocket.WebSocket;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Date;

public class AtsResponseBinary extends AtsResponse {

    private byte[] binaryData;

    public AtsResponseBinary(byte[] bytes) {
        this.binaryData = bytes;
    }

    byte[] toBytes(int i)
    {
        byte[] result = new byte[4];

        result[0] = (byte) (i >> 24);
        result[1] = (byte) (i >> 16);
        result[2] = (byte) (i >> 8);
        result[3] = (byte) (i);

        return result;
    }

    @Override
    public void sendDataToUsbPort(int socketID, WebSocket conn) {
        final byte[] socket = toBytes(socketID);
        final byte[] header = ("HTTP/1.1 200 OK\r\nServer: AtsDroid Driver\r\nDate: " + new Date() + "\r\nContent-type: application/octet-stream\r\nContent-length: "+ this.binaryData.length + "\r\n\r\n").getBytes();
        final byte[] data = this.binaryData;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(socket);
            outputStream.write(header);
            outputStream.write(data);
            conn.send(outputStream.toByteArray());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendDataHttpServer(Socket socket) {
        byte[] header = ("HTTP/1.1 200 OK\r\nServer: AtsDroid Driver\r\nDate: " + new Date() + "\r\nContent-type: application/octet-stream\r\nContent-length: "+ this.binaryData.length + "\r\n\r\n").getBytes();
        BufferedOutputStream bf = null;
        try {
            bf = new BufferedOutputStream(socket.getOutputStream());
            bf.write(header, 0, header.length);

            bf.write(this.binaryData, 0, this.binaryData.length);
        } catch (IOException e) {
            AtsAutomation.sendLogs("Error when sending binary data to udp server:" + e.getMessage() + "\n");
        } finally {
            try {
                if(bf != null) {
                    bf.flush();
                    bf.close();
                }
            }
            catch (Exception e) {
                System.out.println("Error while closing streams" + e);
            }
        }
    }
}
