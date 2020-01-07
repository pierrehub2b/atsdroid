package com.ats.atsdroid.element;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class AtsResponseJSON extends AtsResponse {
    private final static String JSON_RESPONSE_TYPE = "application/json";

    private JSONObject jsonObject = new JSONObject();

    public AtsResponseJSON(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public void sendDataHttpServer(Socket socket) {
        byte[] data = this.jsonObject.toString().getBytes(StandardCharsets.UTF_8);
        byte[] header = ("HTTP/1.1 200 OK\r\nServer: AtsDroid Driver\r\nDate: " + new Date() + "\r\nContent-type: " + JSON_RESPONSE_TYPE + "\r\nContent-length: " + data.length + "\r\n\r\n").getBytes();
        try {
            final BufferedOutputStream bf = new BufferedOutputStream(socket.getOutputStream());
            bf.write(header, 0, header.length);
            bf.write(data, 0, data.length);
            bf.flush();
            bf.close();
        }catch(IOException e){}
    }

    public void sendDataToUsbPort(PrintWriter writer) {
        writer.print(this.jsonObject.toString());
    }
}