package org.redis.protocol;

public enum ResponseCode {
    ERROR(-1),
    OK(0),
    NOT_FOUND(1);

    public int getCode() {
        return code;
    }

    int code;
    ResponseCode(int code) {
        this.code = code;
    }

}
