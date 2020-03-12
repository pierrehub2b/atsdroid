package com.ats.atsdroid.element;

import org.java_websocket.WebSocket;
import java.net.Socket;

public class AtsResponse {
    public final static String RESPONSESPLITTER = "<$atsDroid_ResponseSPLIITER$>";

    public void sendDataHttpServer(Socket socket) {
        //overrided in childs
    }

    public void sendDataToUsbPort(WebSocket conn) {
        //overrided in childs
    }
}
