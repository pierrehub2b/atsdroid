package com.ats.atsdroid.element;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.util.Log;
import android.view.View;
import android.provider.MediaStore;
import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Date;

public class AtsResponseBinary extends AtsResponse {

    private byte[] binaryData;

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
        /*File path = createImageFile(this.screenData);
        if(path != null) {
            writer.print(path.getAbsolutePath());
        String path = SaveImage(this.screenData);
        if(path != "") {
            writer.print(path);
        } else {
            writer.print("error on save");
        }*/

        //byte[] ba = stream.toByteArray();
        //bmp.recycle();

        writer.print(Base64.encodeToString(binaryData, Base64.DEFAULT));
    }

    public static File createImageFile(Bitmap finalBitmap) {
        Context context = InstrumentationRegistry.getTargetContext();
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        File frame = new File(storageDir, "thumbnail.png");
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(frame);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            return frame;
        } catch (Exception e) {
            Log.e("SAVE_IMAGE", e.getMessage(), e);
        }
        return null;
    }
}
