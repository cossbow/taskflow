package com.hikvision.hbfa.sf.handler.call;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.hikvision.hbfa.sf.util.JsonUtil;

import java.util.concurrent.TimeUnit;

public
abstract class ConfigCallable<Config> implements Callable<Config> {

    private final Class<Config> configType;
    private final LoadingCache<String, Config> configCache;

    protected ConfigCallable(Class<Config> configType) {
        this.configType = configType;
        this.configCache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .softValues()
                .build(JsonUtil.cacheLoader(configType));
    }

    public Class<Config> getConfigType() {
        return configType;
    }

    @Override
    public Config parseConfig(String cs) {
        return configCache.get(cs);
    }

}
