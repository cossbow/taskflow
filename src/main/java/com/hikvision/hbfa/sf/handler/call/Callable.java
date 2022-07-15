package com.hikvision.hbfa.sf.handler.call;

import com.hikvision.hbfa.sf.entity.enumeration.CallType;
import com.hikvision.hbfa.sf.util.TypedBean;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Callable<Config> extends TypedBean<CallType> {

    Config parseConfig(String cs);

    CompletableFuture<CallResult> doAsyncCall(CallParam<Config> param);

    default CompletableFuture<CallResult> asyncCall(
            String key, String config, Map<String, Object> input) {
        return doAsyncCall(new CallParam<>(key, parseConfig(config), input));
    }

}
