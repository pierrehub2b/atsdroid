package com.ats.atsdroid.element;
import java.io.PrintWriter;
import java.net.Socket;

public class AtsResponse {
    public final static String RESPONSESPLITTER = "<$atsDroid_ResponseSPLIITER$>";

    public void sendDataHttpServer(Socket socket) {
        //overrided in childs
    }

    public void sendDataToUsbPort(PrintWriter writer) {
        //overrided in childs
    }
}
