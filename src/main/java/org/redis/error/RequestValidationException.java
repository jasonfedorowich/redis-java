package org.redis.error;

import org.redis.validation.Violation;

import java.util.List;
import java.util.Map;

public class RequestValidationException extends RuntimeException {
    public RequestValidationException(Map<Violation.Type, List<Violation>> violations) {
        super("Validation exception with: " + violations.toString());
    }
}
