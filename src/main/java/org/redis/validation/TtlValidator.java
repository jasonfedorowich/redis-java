package org.redis.validation;

import static org.redis.validation.Numeric.isNotLong;

public class TtlValidator implements Validator{

    @Override
    public Violation validate(String value) {
        if(value.equals("-1")) return new Violation("", Violation.Type.OK);
        else if(isNotLong(value)){
            return new Violation("Ttl candidate is not an integer", Violation.Type.TTL);
        }else{
            return new Violation("", Violation.Type.OK);
        }
    }
}
