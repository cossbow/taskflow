package com.hikvision.hbfa.sf.kafka;

import java.util.concurrent.CompletableFuture;

public interface KafkaRecvHandler<Param, Data> {

    CompletableFuture<?> receive(Param param, Data data);

}
