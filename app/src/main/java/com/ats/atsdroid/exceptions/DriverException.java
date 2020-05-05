package com.ats.atsdroid.exceptions;

public class DriverException extends Exception {

    public static final String UNAVAILABLE_FEATURE = "Feature is unavailable";
    public static final String UNKNOWN_FUNCTION = "Feature is unavailable";
    public static final String UNKNOWN_ERROR = "Driver error";

     public DriverException(String message) {
        super(message);
    }
}
