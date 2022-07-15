package com.hikvision.hbfa.sf.handler.call.kafka;

import com.hikvision.hbfa.sf.entity.enumeration.CallType;
import com.hikvision.hbfa.sf.entity.enumeration.ResultCode;
import com.hikvision.hbfa.sf.handler.call.CallParam;
import com.hikvision.hbfa.sf.handler.call.CallResult;
import com.hikvision.hbfa.sf.handler.call.Callable;
import com.hikvision.hbfa.sf.handler.call.ConfigCallable;
import com.hikvision.hbfa.sf.kafka.KafkaPublisher;
import com.hikvision.hbfa.sf.util.ValueUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class KafkaCallable
        extends ConfigCallable<KafkaConfig>
        implements Callable<KafkaConfig> {

    private final KafkaPublisher publisher;

    public KafkaCallable(KafkaPublisher kafkaPublisher) {
        super(KafkaConfig.class);
        this.publisher = kafkaPublisher;
    }

    @Override
    public CallType type() {
        return CallType.KAFKA;
    }


    @Override
    public CompletableFuture<CallResult> doAsyncCall(CallParam<KafkaConfig> param) {
        var config = param.getConfig();
        if (ValueUtil.isEmpty(config.getTopic())) {
            return CompletableFuture.completedFuture(
                    new CallResult(ResultCode.BAD_CONFIG, "topic empty"));
        }
        return publisher.send(config.getTopic(), param.getInput())
                .thenApply(m -> new CallResult());
    }
}
