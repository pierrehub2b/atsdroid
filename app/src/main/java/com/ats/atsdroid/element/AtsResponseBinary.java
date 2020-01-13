package com.ats.atsdroid.element;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.PrintWriter;
import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Date;

public class AtsResponseBinary extends AtsResponse {
    private byte[] binaryData = new byte[0];

    public AtsResponseBinary(byte[] bytes) {
        this.binaryData = bytes;
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

    public void sendDataToUsbPort(PrintWriter writer) {
        writer.print(new String(this.binaryData));
    }
}
