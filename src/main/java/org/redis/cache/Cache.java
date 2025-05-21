package org.redis.cache;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

//todo make cache persist data so on restart it can load data
public class Cache {
    private final Map<String, Node> cache = new ConcurrentHashMap<>();

    public Node get(String key) {
        return cache.get(key);
    }

    public void set(String key, Node value) {
        cache.put(key, value);
        System.out.println(cache);
    }

    public Node delete(String key) {
        return cache.remove(key);
    }
}
