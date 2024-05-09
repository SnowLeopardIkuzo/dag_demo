package com.example.dag.frame.exception;

public class DagException extends RuntimeException {
    public DagException(String message) {
        super(message);
    }

    public DagException(String message, Throwable cause) {
        super(message, cause);
    }
}
