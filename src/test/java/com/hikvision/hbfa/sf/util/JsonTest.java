package com.hikvision.hbfa.sf.util;

import lombok.Data;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ThreadLocalRandom;

public class JsonTest {

    @Data
    static class T {
        private long id;
        private transient String tag;
    }


    @Test
    public void testCharSequence() {
        var s1 = ValueUtil.newUUID();
        var s2 = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
        var ls = LazyToString.of(() -> s1 + s2);
        var s = JsonUtil.toJson(ls);
        Assert.assertEquals('"' + s1 + s2 + '"', s);
    }

    @Test
    public void testTransient() {
        var t0 = new T();
        t0.setId(ThreadLocalRandom.current().nextLong(0, Integer.MAX_VALUE));
        t0.setTag(ValueUtil.newUUID());
        var s = JsonUtil.toJson(t0);
        var t1 = JsonUtil.fromJson(s, T.class);
        Assert.assertNull(t1.getTag());
    }

}
