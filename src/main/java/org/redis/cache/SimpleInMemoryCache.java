package org.redis.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleInMemoryCache implements KVCache {
    private final Map<String, byte[]> cache = new ConcurrentHashMap<>();
    @Override
    public byte[] get(String key) {
        return cache.get(key);
    }

    @Override
    public void set(String key, byte[] value) {

        cache.put(key, value);
        System.out.println(cache);
    }

    @Override
    public byte[] delete(String key) {
        System.out.println(cache);
        return cache.remove(key);
    }
}
