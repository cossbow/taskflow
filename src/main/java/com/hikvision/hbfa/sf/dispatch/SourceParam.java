package com.hikvision.hbfa.sf.dispatch;

import lombok.Data;

@Data
public class SourceParam {
    private final String name;
    private final String workflow;
    private final String config;
    private final int batch;
    private final long timeout;
    private final int retries;
}
