package org.redis.error;

public class MissingFileException extends RuntimeException{

    public MissingFileException(String msg) {
        super(msg);
    }
}
