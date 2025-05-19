package org.redis.protocol;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public abstract class Request {

    protected final List<String> args;

    abstract boolean isReadOnly();

    public String key(){
        return args.get(0);
    }

    public String value(){
        throw new UnsupportedOperationException();
    }

    abstract long ttl();

}
