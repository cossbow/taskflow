package com.hikvision.hbfa.sf.util;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class ExpiringCacheTest {

    @Test
    public void testGetSet() {
        var cache = new ExpiringCache<String, Object>(Integer.MAX_VALUE);
        var key = ValueUtil.newUUID();
        Assert.assertNull(cache.get(key));
        var value = ValueUtil.newUUID();
        cache.set(key, value);
        Assert.assertEquals(value, cache.get(key));
    }

    @Test
    public void testExpiresDefault() {
        var secs = ThreadLocalRandom.current().nextInt(1, 10);

        {
            var cache = new ExpiringCache<String, Object>(secs * 1000L);
            var key = ValueUtil.newUUID();
            var value = ValueUtil.newUUID();
            var ef = cache.set(key, value);
            // 还未过期
            Assert.assertEquals(value, cache.get(key));
            var t0 = Instant.now();
            // 等待过期
            var entry = ef.join();
            var t1 = Instant.now();
            // 时间应该差不多
            Assert.assertEquals(secs, (t1.getEpochSecond() - t0.getEpochSecond()));

            Assert.assertEquals(key, entry.getKey());
            Assert.assertEquals(value, entry.getValue());
        }
        {
            var cache = new ExpiringCache<String, Object>(Integer.MAX_VALUE);
            var key = ValueUtil.newUUID();
            var value = ValueUtil.newUUID();
            var ef = cache.set(key, value, secs * 1000L);
            // 还未过期
            Assert.assertEquals(value, cache.get(key));
            var t0 = Instant.now();
            // 等待过期
            var entry = ef.join();
            var t1 = Instant.now();
            // 时间应该差不多
            Assert.assertEquals(secs, (t1.getEpochSecond() - t0.getEpochSecond()));

            Assert.assertEquals(key, entry.getKey());
            Assert.assertEquals(value, entry.getValue());
        }
    }


}
