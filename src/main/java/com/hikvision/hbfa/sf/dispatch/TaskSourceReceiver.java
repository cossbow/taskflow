package com.hikvision.hbfa.sf.dispatch;

import com.hikvision.hbfa.sf.entity.json.ObjectMap;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface TaskSourceReceiver {

    CompletableFuture<?> receive(SourceParam param, ObjectMap input);

}
