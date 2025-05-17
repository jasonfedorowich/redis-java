package org.redis.error;

import java.nio.BufferUnderflowException;

public class InvalidRequestFormat extends RuntimeException {

    public InvalidRequestFormat(String message) {
        super(message);
    }

    public InvalidRequestFormat(BufferUnderflowException e) {
        super(e);
    }
}
