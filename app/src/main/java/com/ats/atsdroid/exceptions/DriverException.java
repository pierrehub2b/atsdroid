package com.ats.atsdroid.exceptions;

public class DriverException extends Exception {

    public static final String UNAVAILABLE_FEATURE = "Unavailable command";
    public static final String UNKNOWN_FUNCTION = "Unknown command";
    public static final String UNKNOWN_ERROR = "Driver error";

     public DriverException(String message) {
        super(message);
    }
}
