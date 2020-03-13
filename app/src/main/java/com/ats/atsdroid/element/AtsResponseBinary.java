package com.ats.atsdroid.element;

import android.util.Base64;

import com.ats.atsdroid.utils.AtsAutomation;

import org.java_websocket.WebSocket;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

public class AtsResponseBinary extends AtsResponse {
    private static final int PACKET_SIZE = 2000;
    private byte[] binaryData;

    public AtsResponseBinary(byte[] bytes) {
        this.binaryData = bytes;
    }

    @Override
    public void sendDataToUsbPort(WebSocket conn) {
        byte[] header = ("HTTP/1.1 200 OK\r\nServer: AtsDroid Driver\r\nDate: " + new Date() + "\r\nContent-type: application/octet-stream\r\nContent-length: "+ this.binaryData.length + "\r\n\r\n").getBytes();
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(header);
            outputStream.write(this.binaryData);

            conn.send(ByteBuffer.wrap(outputStream.toByteArray()));
            outputStream.close();
        } catch(IOException e) {
            AtsAutomation.sendLogs("Error when sending binary data to udp server:" + e.getMessage());
        }
    }

    public void sendDataHttpServer(Socket socket) {
        byte[] header = ("HTTP/1.1 200 OK\r\nServer: AtsDroid Driver\r\nDate: " + new Date() + "\r\nContent-type: application/octet-stream\r\nContent-length: "+ this.binaryData.length + "\r\n\r\n").getBytes();
        BufferedOutputStream bf = null;
        try {
            bf = new BufferedOutputStream(socket.getOutputStream());
            bf.write(header, 0, header.length);

            /*int dataLength = this.binaryData.length;
            int packetSize = PACKET_SIZE;
            int currentPos = 0;

            sendData(this.binaryData, currentPos, dataLength, packetSize, bf);

            while (dataLength > 0) {
                currentPos += packetSize;
                dataLength -= packetSize;
                if (dataLength < packetSize) {
                    packetSize = dataLength;
                }
                sendData(this.binaryData, currentPos, dataLength, packetSize, bf);
            }*/

            bf.write(this.binaryData, 0, this.binaryData.length);
        }catch(IOException e){
            AtsAutomation.sendLogs("Error when sending binary data to udp server:" + e.getMessage());
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

    public void sendDataToUsbPort(PrintWriter writer) {
        writer.print(RESPONSESPLITTER + Base64.encodeToString(binaryData, Base64.NO_WRAP) + "\u001a");
    }

    private void sendData(byte[] screen, int currentPos, int dataLength, int packetSize, BufferedOutputStream bf) throws IOException{
        final byte[] send = getData(currentPos, dataLength, packetSize);
        System.arraycopy(screen, currentPos, send, 8, packetSize);
        bf.write(send, currentPos, send.length);
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

}
