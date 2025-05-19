package org.redis.cache;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Node {

    private final byte[] value;
    private long ttl = -1;


}
