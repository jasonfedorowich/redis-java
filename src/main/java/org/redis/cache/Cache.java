package org.redis.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//todo make cache persist data so on restart it can load data
public class Cache {
    private final Map<String, byte[]> cache = new ConcurrentHashMap<>();

    public byte[] get(String key) {
        return cache.get(key);
    }

    public void set(String key, byte[] value) {

        cache.put(key, value);
        System.out.println(cache);
    }

    public byte[] delete(String key) {
        System.out.println(cache);
        return cache.remove(key);
    }
}
