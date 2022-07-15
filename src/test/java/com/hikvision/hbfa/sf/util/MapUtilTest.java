package com.hikvision.hbfa.sf.util;

import com.hikvision.hbfa.sf.entity.json.ObjectMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class MapUtilTest {

    long randInt() {
        return ThreadLocalRandom.current().nextLong();
    }

    String randStr() {
        return ValueUtil.newUUID();
    }

    HashMap<Object, Object> genHashMap() {
        var origin = new HashMap<>();
        origin.put("id", randInt());
        origin.put("name", randStr());
        origin.put("book", new ObjectMap("title", randStr(), "price", randInt()));
        return origin;
    }

    @Test
    public void testDeepMerge() {
        var origin = genHashMap();
        var replace = Map.<Object, Object>of("id", randInt());
        MapUtil.deepMerge(origin, replace);
        Assert.assertEquals(replace.get("id"), origin.get("id"));

        origin = genHashMap();
        replace = Map.of("name", randStr());
        MapUtil.deepMerge(origin, replace);
        Assert.assertEquals(replace.get("name"), origin.get("name"));

        origin = genHashMap();
        var price = ((Map) origin.get("book")).get("price");
        replace = Map.of("book", Map.of("title", randStr()));
        MapUtil.deepMerge(origin, replace);
        Assert.assertTrue(replace.get("book") instanceof Map);
        Assert.assertTrue(origin.get("book") instanceof Map);
        Assert.assertEquals(((Map) replace.get("book")).get("title"), ((Map) origin.get("book")).get("title"));
        Assert.assertEquals(price, ((Map) origin.get("book")).get("price"));
    }
}
