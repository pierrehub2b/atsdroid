package com.ats.atsdroid.exceptions;

public class SyntaxException extends Exception {

    public static final String INVALID_PARAMETER = "Bad parameter";
    public static final String INVALID_METHOD = "Bad syntax";

    public SyntaxException(String message) {
        super(message);
    }
}
