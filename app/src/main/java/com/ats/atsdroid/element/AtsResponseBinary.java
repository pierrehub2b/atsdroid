package com.ats.atsdroid.element;
import android.graphics.Bitmap;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.PrintWriter;
import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class AtsResponseBinary extends AtsResponse {
    private byte[] binaryData;
    private Bitmap screenCapture;

    public AtsResponseBinary(byte[] bytes, Bitmap screenCapture) {
        this.binaryData = bytes;
        this.screenCapture = screenCapture;
    }

    public void sendDataHttpServer(Socket socket) {
        byte[] header = ("HTTP/1.1 200 OK\r\nServer: AtsDroid Driver\r\nDate: " + new Date() + "\r\nContent-type: application/octet-stream\r\nContent-length: " + this.binaryData.length + "\r\n\r\n").getBytes();
        try {
            final BufferedOutputStream bf = new BufferedOutputStream(socket.getOutputStream());
            bf.write(header, 0, header.length);
            bf.write(this.binaryData, 0, this.binaryData.length);
            bf.flush();
            bf.close();
        }catch(IOException e){}
    }

     /*public void sendDataToUsbPort(PrintWriter writer) {
        String strOutput = new String(this.binaryData);
        writer.println(strOutput);
    }

    public void sendDataToUsbPort(PrintWriter writer) {
        writer.println(new String(this.binaryData));
        writer.flush();
        writer.close();
    }*/

    public void sendDataToUsbPort(PrintWriter writer) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("data", new String(this.binaryData, StandardCharsets.UTF_8));
            obj.put("width", screenCapture.getWidth());
            obj.put("height", screenCapture.getHeight());
            writer.print(obj.toString());
        } catch (Exception ex) {}
    }
}
