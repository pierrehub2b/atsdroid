package com.ats.atsdroid.exceptions;

public class SyntaxException extends Exception {

    public static final String INVALID_PARAMETER = "Invalid parameter";
    public static final String INVALID_METHOD = "Invalid method";

    public SyntaxException(String message) {
        super(message);
    }
}
