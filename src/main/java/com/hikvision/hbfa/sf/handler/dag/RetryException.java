package com.hikvision.hbfa.sf.handler.dag;

public class RetryException extends RuntimeException {
    public RetryException() {
    }

    public RetryException(String message) {
        super(message);
    }

    public RetryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryException(Throwable cause) {
        super(cause);
    }
}
