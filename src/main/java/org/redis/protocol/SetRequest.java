package org.redis.protocol;

import org.redis.validation.TtlValidator;
import org.redis.validation.Validate;

import java.util.List;


public class SetRequest extends Request {

    @Validate(validator=TtlValidator.class)
    private final String ttl;

    public SetRequest(List<String> args) {
        super(3, args);
        this.ttl = args.size() >= 3 ? args.get(2) : "-1";
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
