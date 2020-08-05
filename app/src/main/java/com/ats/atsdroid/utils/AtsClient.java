package com.ats.atsdroid.utils;

public class AtsClient {

    public final String token;
    public final String userAgent;
    public final String ipAddress;

    public static AtsClient current;

    public AtsClient(String token, String userAgent, String ipAddress) {
        this.token = token;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }
}
