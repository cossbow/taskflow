package com.hikvision.hbfa.sf.ext.voice;

import lombok.Data;

@Data
public class VoiceBaseResult<D> {
    private int errorCode;
    private String errorMsg;

    public D data() {
        return null;
    }
}
