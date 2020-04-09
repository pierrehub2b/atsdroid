package com.ats.atsdroid.element;

import com.ats.atsdroid.utils.AtsAutomation;

import org.java_websocket.WebSocket;

import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Date;

public class AtsResponseBinary extends AtsResponse {
    // private static final int PACKET_SIZE = 2000;
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
        result[3] = (byte) (i /*>> 0*/);

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

    /* public void sendDataToUsbPort(PrintWriter writer) {
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
    } */

}
