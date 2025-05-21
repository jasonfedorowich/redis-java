package org.redis.protocol;

import lombok.RequiredArgsConstructor;

import java.io.DataInputStream;
import java.util.List;


public class SetRequest extends Request {

    public SetRequest(List<String> args) {
        super(args);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }


    public String value(){
        return args.get(1);
    }

    @Override
    public long ttl() {
        return args.size() >= 3 ? Long.parseLong(args.get(2)) : -1;
    }
}
