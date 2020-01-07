package com.ats.atsdroid.element;
import java.io.PrintWriter;
import java.net.Socket;

public class AtsResponse {
    public void sendDataHttpServer(Socket socket) {
        //overrided in childs
    }

    public void sendDataToUsbPort(PrintWriter writer) {
        //overrided in childs
    }
}
