package org.redis.protocol;

import lombok.RequiredArgsConstructor;

import java.io.DataInputStream;
import java.util.List;

@RequiredArgsConstructor
public class GetRequest implements Request {

    private final List<String> args;


    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String key() {
        return this.args.get(0);
    }
}
