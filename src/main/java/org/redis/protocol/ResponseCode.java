package org.redis.protocol;

public enum ResponseCode {
    ERROR(0),
    OK(1),
    NOT_FOUND(2);

    public int getCode() {
        return code;
    }

    int code;
    ResponseCode(int code) {
        this.code = code;
    }

}
