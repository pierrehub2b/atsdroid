package com.ats.atsdroid.server;

import android.os.Build;
import android.support.annotation.RequiresApi;

import com.ats.atsdroid.element.AtsResponse;
import com.ats.atsdroid.element.AtsResponseJSON;
import com.ats.atsdroid.utils.AtsAutomation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

public class AtsHttpServer implements Runnable {
    private final static String CONTENT_LENGTH = "Content-Length: ";
    private final static String USER_AGENT = "User-Agent: ";

    private Socket socket;
    private AtsAutomation automation;

    public AtsHttpServer(Socket socket, AtsAutomation automation) {
        this.socket = socket;
        this.automation = automation;
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            String line;
            String userAgent = socket.getInetAddress().getHostAddress();
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
                response.sendDataHttpServer(socket);
            } else{
                new AtsResponseJSON(new JSONObject("{\"status\":\"-11\",\"message\":\"unknown command\"}")).sendDataHttpServer(socket);
            }

        } catch (IOException | JSONException e) {
            AtsAutomation.sendLogs("IOError or JSONException on HttpServer:" + e.getMessage());
        } finally {
            try {
                if(in != null) {
                    in.close();
                }
                socket.close(); // we close socket connection
            } catch (Exception e) {
                AtsAutomation.sendLogs("Cannot close HTTPServer:" + e.getMessage());
            }
        }
    }
}