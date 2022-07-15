package com.hikvision.hbfa.sf.handler.dag;

public class CallTimeoutException extends RetryException {
    public CallTimeoutException() {
    }

    public CallTimeoutException(String message) {
        super(message);
    }

    public CallTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public CallTimeoutException(Throwable cause) {
        super(cause);
    }
}
