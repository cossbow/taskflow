package com.hikvision.hbfa.sf.ex;

public class HttpStatusException extends RuntimeException {
    private final int status;

    public HttpStatusException(int status) {
        this(null, status);
    }

    public HttpStatusException(String message, int status) {
        super(null != message ? message : "HTTP " + status);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
