package org.redis.protocol;

import lombok.RequiredArgsConstructor;

import java.io.DataInputStream;
import java.util.List;

@RequiredArgsConstructor
public class SetRequest implements Request {

    private final List<String> args;

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String key() {
        return args.get(0);
    }

    public String value(){
        return args.get(1);
    }
}
