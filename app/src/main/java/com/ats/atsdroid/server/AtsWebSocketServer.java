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
    public void onError(WebSocket conn, Exception ex) {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        try {
            final BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(message.array())));
            final String userAgent = conn.getRemoteSocketAddress().getHostString();

            final RequestType request = RequestType.generate(in, userAgent);

            if(request != null) {
                final AtsResponse response = automation.executeRequest(request, false);
                response.sendDataToUsbPort(conn);
            } else{
                final AtsResponse response = new AtsResponseJSON(new JSONObject("{\"status\":\"-11\",\"message\":\"unknown command\"}"));
                response.sendDataToUsbPort(conn);
            }

        } catch (IOException | JSONException e) {
            AtsAutomation.sendLogs("IOError or JSONException on HttpServer:" + e.getMessage());
        }
    }
}