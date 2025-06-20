package org.redis.protocol;

import lombok.ToString;

import java.util.List;

public class GetRequest extends Request {

    public GetRequest(List<String> args) {
        super(3, args);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public String key() {
        return this.args.get(0);
    }


}
