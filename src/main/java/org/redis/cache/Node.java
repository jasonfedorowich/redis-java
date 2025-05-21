package org.redis.cache;

import lombok.*;

@RequiredArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@ToString
public class Node {

    private final byte[] value;
    private long lastUsed;
    private long ttl = -1;



}
