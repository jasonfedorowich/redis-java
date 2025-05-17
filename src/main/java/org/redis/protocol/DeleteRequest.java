package org.redis.protocol;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class DeleteRequest implements Request{

    private final List<String> args;

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String key() {
        return args.get(0);
    }
}
