package com.bugreport.exception;

public class BugNotFoundException extends RuntimeException {
    public BugNotFoundException(String message) {
        super(message);
    }
}
