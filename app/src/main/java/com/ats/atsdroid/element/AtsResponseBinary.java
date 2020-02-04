package com.ats.atsdroid.element;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;

import com.ats.atsdroid.utils.AtsAutomation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Date;

public class AtsResponseBinary extends AtsResponse {
    private byte[] binaryData;
    private Bitmap screenData;

    public AtsResponseBinary(byte[] bytes, Bitmap screenData) {
        this.binaryData = bytes;
        this.screenData = screenData;
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
        String path = SaveImage(this.screenData);
        if(path != "") {
            writer.print(path);
        } else {
            writer.print("error on save");
        }
    }

    private String SaveImage(Bitmap finalBitmap) {
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root);
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        String frame = "Image-capture.png";
        File file = new File (myDir, frame);
        if (file.exists ())
            file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            return root + "/" + frame;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
