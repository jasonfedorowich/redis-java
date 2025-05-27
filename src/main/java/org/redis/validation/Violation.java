package org.redis.validation;


public record Violation(String message, org.redis.validation.Violation.Type type) {

    public enum Type {
        OK,
        TTL
    }

}
