package org.redis.protocol;

import lombok.RequiredArgsConstructor;

import java.util.List;

public class DeleteRequest extends Request{

    public DeleteRequest(List<String> args) {
        super(args);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String key() {
        return args.get(0);
    }

    @Override
    long ttl() {
        return 0;
    }
}
