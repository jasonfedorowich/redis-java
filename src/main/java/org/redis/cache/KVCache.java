package org.redis.cache;

public interface KVCache {

    byte[] get(String key);

    void set(String key, byte[] value);

    byte[] delete(String key);

    static KVCache newSimpleInMemoryCache(){
        return new SimpleInMemoryCache();
    }
}
