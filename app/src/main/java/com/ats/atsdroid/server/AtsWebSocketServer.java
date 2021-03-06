package com.ats.atsdroid.server;

import android.util.Log;

import com.ats.atsdroid.response.AtsResponse;
import com.ats.atsdroid.response.AtsResponseJSON;
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
import java.util.Arrays;

public class AtsWebSocketServer extends WebSocketServer {

    private final AtsAutomation automation;
    private WebSocket gestureCatcherSocket;
    private WebSocket mobileStationSocket;

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
            // final int webSocketSource = message.getInt();
            
            // if (webSocketSource == 0) { // source = Mobile Station
                mobileStationSocket = conn;
                
                final int socketID = message.getInt();
                final byte[] mess = Arrays.copyOfRange(message.array(), 4, message.array().length);
                final BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(mess)));
                final String userAgent = conn.getRemoteSocketAddress().getHostString();
                final RequestType request = RequestType.generate(in, userAgent);
    
                final AtsResponse response;
                if (request == null) {
                    response = new AtsResponseJSON(new JSONObject("{\"status\":\"-11\",\"message\":\"unknown command\"}"));
                } else {
                    response = automation.executeRequest(request);
                }
    
                response.sendDataToUsbPort(socketID, conn);
            /*
            } else {
            
                gestureCatcherSocket = conn;
                
                final byte[] file = Arrays.copyOfRange(message.array(), 4, message.array().length);
                mobileStationSocket.send(file);
            }
            */

        } catch (IOException | JSONException e) {
            AtsAutomation.sendLogs("IOError or JSONException on HttpServer:" + e.getMessage() + "\n");
        }
    }
}