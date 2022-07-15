package com.hikvision.hbfa.sf.ex;

public class NotFoundException extends IllegalArgumentException {
    public NotFoundException() {
    }

    public NotFoundException(String message) {
        super(message);
    }

}
