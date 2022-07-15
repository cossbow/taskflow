package com.hikvision.hbfa.sf.entity.json;

import java.util.HashMap;
import java.util.Map;

public class ObjectMap extends HashMap<String, Object> {
    public ObjectMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ObjectMap(int initialCapacity) {
        super(initialCapacity);
    }

    public ObjectMap() {
    }

    public ObjectMap(Map<? extends String, ?> m) {
        super(m);
    }

    public ObjectMap(String key, Object value) {
        super(1);
        put(key, value);
    }

    public ObjectMap(String k1, Object v1, String k2, Object v2) {
        super(2);
        put(k1, v1);
        put(k2, v2);
    }

}
