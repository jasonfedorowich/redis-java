package org.redis.protocol;

import java.util.List;

public class DeleteRequest extends Request{

    public DeleteRequest(List<String> args) {
        super(3, args);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String key() {
        return args.get(0);
    }

}
