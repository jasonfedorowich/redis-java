package org.redis.validation;

public interface Validator {

    Violation validate(String value);


}
