package org.redis.protocol;

public interface Request {

    boolean isReadOnly();

    String key();

}
