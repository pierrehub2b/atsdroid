package com.ats.atsdroid.server;

import android.util.Log;

import com.ats.atsdroid.element.AtsResponse;
import com.ats.atsdroid.element.AtsResponseJSON;
import com.ats.atsdroid.utils.AtsAutomation;

import com.ats.atsdroid.utils.AtsClient;
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
import java.util.Arrays;

public class AtsWebSocketServer extends WebSocketServer {

    private AtsAutomation automation;

    public AtsWebSocketServer(InetSocketAddress address, AtsAutomation automation) {
        super(address);
        this.automation = automation;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Log.d("WSS", "WebSocket Server get connection");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.d("WSS", "WebSocket Server closed : " + reason);
        AtsAutomation.sendLogs("ATS_WEB_SOCKET_SERVER_STOP\n");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        final byte[] screenshot = automation.getScreenData();
        // Log.d("WSS", "Capture web socket send " + screenshot.length);
        conn.send(screenshot);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.e("WSS", "WebSocket Server error : " + ex);
        AtsAutomation.sendLogs("ATS_WEB_SOCKET_SERVER_ERROR:" + ex + "\n");
    }

    @Override
    public void onStart() {
        AtsAutomation.sendLogs("ATS_WEB_SOCKET_SERVER_START:" + this.getPort() + "\n");
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {

        try {
            final int socketID = message.getInt();
            final byte[] mess = Arrays.copyOfRange(message.array(), 4, message.array().length);

            final BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(mess)));
            final String userAgent = conn.getRemoteSocketAddress().getHostString();
            final RequestType request = RequestType.generate(in, userAgent);

            final AtsResponse response;
            if (request == null) {
                response = new AtsResponseJSON(new JSONObject("{\"status\":\"-11\",\"message\":\"unknown command\"}"));
            } else {
                if (AtsClient.current != null) {
                    if (request.token == null) {
                        response = new AtsResponseJSON(new JSONObject("{\"status\":\"-20\",\"message\":\"Device already in use : " + AtsClient.current.userAgent + "\"}"));
                    } else {
                        if (request.token.equals(AtsClient.current.token)) {
                            response = automation.executeRequest(request);
                        } else {
                            response = new AtsResponseJSON(new JSONObject("{\"status\":\"-20\",\"message\":\"Device already in use : " + AtsClient.current.userAgent + "\"}"));
                        }
                    }
                } else {
                    response = automation.executeRequest(request);
                }
            }

            response.sendDataToUsbPort(socketID, conn);

        } catch (IOException | JSONException e) {
            AtsAutomation.sendLogs("IOError or JSONException on HttpServer:" + e.getMessage() + "\n");
        }
    }
}