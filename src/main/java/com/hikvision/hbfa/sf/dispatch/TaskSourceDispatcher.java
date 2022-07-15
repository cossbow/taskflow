package com.hikvision.hbfa.sf.dispatch;

import com.hikvision.hbfa.sf.entity.enumeration.TaskSourceStatus;

import java.util.concurrent.CompletableFuture;

public interface TaskSourceDispatcher {

    void start();

    CompletableFuture<Void> stop();

    TaskSourceStatus status();

}
