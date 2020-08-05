package com.ats.atsdroid.response;

import com.ats.atsdroid.utils.AtsAutomation;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class AtsResponseJSON extends AtsResponse {
    private final static String JSON_RESPONSE_TYPE = "application/json";

    private JSONObject jsonObject;

    public AtsResponseJSON(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    private byte[] getData() {
        return this.jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] getHeader() {
        return ("HTTP/1.1 200 OK\r\nServer: AtsDroid Driver\r\nDate: " + new Date() + "\r\nContent-type: " + JSON_RESPONSE_TYPE + "\r\nContent-length: " + getData().length + "\r\n\r\n").getBytes();
    }

    public void sendDataHttpServer(Socket socket) {
        byte[] data = getData();
        byte[] header = getHeader();
        try {
            final BufferedOutputStream bf = new BufferedOutputStream(socket.getOutputStream());
            bf.write(header, 0, header.length);
            bf.write(data, 0, data.length);
            bf.flush();
            bf.close();
        }catch(IOException e){
            AtsAutomation.sendLogs("Error when sending binary data to udp server:" + e.getMessage() + "\n");
        }
    }

    public void sendDataToUsbPort(int socketID, WebSocket conn) {
        final byte[] header = getHeader();
        final byte[] data = getData();

        final ByteBuffer buffer = ByteBuffer.allocate(4 + header.length + data.length);
        buffer.putInt(socketID);
        buffer.put(header);
        buffer.put(data);

        conn.send(buffer.array());
    }

    /* public void sendDataToUsbPort(PrintWriter writer) {
        String data = this.jsonObject.toString();
        int dataLen = data.length();
        writer.print("\u0010AtsContentLength:" +  dataLen + "\u0011");
        writer.print(data);
        writer.flush();
    } */
}