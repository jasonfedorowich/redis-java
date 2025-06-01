package org.redis.cache;

import lombok.*;

import java.io.Serializable;

@RequiredArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@ToString
public class Node implements Serializable {

    private final byte[] value;
    private long lastUsed;
    private long ttl = -1;



}
