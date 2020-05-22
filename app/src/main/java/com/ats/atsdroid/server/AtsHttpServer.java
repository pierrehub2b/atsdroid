package com.ats.atsdroid.server;

import android.os.Build;
import android.support.annotation.RequiresApi;

import com.ats.atsdroid.element.AtsResponse;
import com.ats.atsdroid.element.AtsResponseJSON;
import com.ats.atsdroid.utils.AtsAutomation;

import com.ats.atsdroid.utils.AtsClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class AtsHttpServer implements Runnable {
    private final Socket socket;
    private final AtsAutomation automation;

    public AtsHttpServer(Socket socket, AtsAutomation automation) {
        this.socket = socket;
        this.automation = automation;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            final String userAgent = socket.getInetAddress().getHostAddress();
            final RequestType request = RequestType.generate(in, userAgent);

            final AtsResponse response;
            if (request == null) {
                response = new AtsResponseJSON(new JSONObject("{\"status\":\"-11\",\"message\":\"unknown command\"}"));
            } else {
                response = automation.executeRequest(request);
            }

            response.sendDataHttpServer(socket);
        } catch (IOException | JSONException e) {
            AtsAutomation.sendLogs("IOError or JSONException on HttpServer:" + e.getMessage() + "\n");
        } finally {
            try {
                if(in != null) {
                    in.close();
                }
                socket.close(); // we close socket connection
            } catch (Exception e) {
                AtsAutomation.sendLogs("Cannot close HTTPServer:" + e.getMessage() + "\n");
            }
        }
    }
}