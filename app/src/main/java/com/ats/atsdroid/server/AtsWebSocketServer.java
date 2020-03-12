package com.ats.atsdroid.server;

import com.ats.atsdroid.element.AtsResponse;
import com.ats.atsdroid.element.AtsResponseJSON;
import com.ats.atsdroid.utils.AtsAutomation;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class AtsWebSocketServer extends WebSocketServer {

    // To refactor
    private final static String CONTENT_LENGTH = "Content-Length: ";
    private final static String USER_AGENT = "User-Agent: ";

    private AtsAutomation automation;

    public AtsWebSocketServer(InetSocketAddress address, AtsAutomation automation) {
        super(address);

        this.automation = automation;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send("Server started !");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

    }

    @Override
    public void onMessage(WebSocket conn, String message) {

    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        BufferedReader in = null;
        try {
            in =new BufferedReader(new InputStreamReader(new ByteArrayInputStream(message.array())));

            String line;
            String userAgent = conn.getRemoteSocketAddress().getHostString();
            int contentLength = 0;

            String input = in.readLine();

            while (!(line = in.readLine()).equals("")) {
                if (line.startsWith(CONTENT_LENGTH)) {
                    try {
                        contentLength = Integer.parseInt(line.substring(CONTENT_LENGTH.length()));
                    }catch(NumberFormatException e){
                        AtsAutomation.sendLogs("Error number format expression on HttpServer:" + e.getMessage());
                    }
                }else if(line.startsWith(USER_AGENT)){
                    userAgent = line.substring(USER_AGENT.length()) + " " + userAgent;
                }
            }

            String postData = "";
            if (contentLength > 0) {
                char[] charArray = new char[contentLength];
                in.read(charArray, 0, contentLength);
                postData = new String(charArray);
            }

            if(input != null) {
                final AtsResponse response = automation.executeRequest(new RequestType(input, postData, userAgent), false);
                response.sendDataToUsbPort(conn);
            } else{
                new AtsResponseJSON(new JSONObject("{\"status\":\"-11\",\"message\":\"unknown command\"}")).sendDataToUsbPort(conn);
            }

        } catch (IOException | JSONException e) {
            AtsAutomation.sendLogs("IOError or JSONException on HttpServer:" + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    @Override
    public void onStart() {

    }
}
